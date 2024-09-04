# How to use

- build and publish the plugin to `mavenLocal`, there is a run configuration included or "run anything" could be used:

```bash
./gradlew model:publishToMavenLocal
./gradlew plugin:publishToMavenLocal
```

- get the sample project from [here](https://github.com/pratclot/kotlinproject)
- run the analyzer app with included run configuration. The first argument to the App is the path to the KMP project to
  analyze, so edit that accordingly
- I wish I could tell you how to run the app from cmdline. A simple `./gradlew app:run` will say
  `Error: Could not find or load main class org.example.AppKt`, and the command invoked by the run configuration does
  not fit into one output's screen :) I guess one could build the jar and all, but it does not seem like the main point
  of the exercise

# How it was done

- first, I used the [wizard](https://kmp.jetbrains.com/) to create a sample project to analyze
- then I asked ChatGPT to implement the solution
- I used the generated "code" as the basis for the app. It however would not work as intended, largely because there are
  no source sets exposed by the Gradle Tooling API by default or with their built-in models
- the source sets are exposed via `kotlin` extension that is created and populated by Kotlin Gradle plugin.
  IDEA [does](https://github.com/JetBrains/intellij-community/blob/be49cda40d214c910b9254e2dfbbbfada56c1e5e/plugins/kotlin/gradle/gradle-tooling/impl/src/org/jetbrains/kotlin/idea/gradleTooling/reflect/KotlinExtensionReflection.kt#L20)
  a reflection on it to get the source sets
- `pill`
  also [gets](https://github.com/JetBrains/kotlin/blob/e67042118adacb38d27d5a0a94357ff1c7b96acd/plugins/pill/pill-importer/src/ModelParser.kt#L344)
  the source sets by reflection. Seems straightforward, just need to get access to Gradle's `Project`, which of course
  Gradle "Tooling" API will not give for free
- [here](https://github.com/bmuschko/tooling-api-custom-model/blob/master/plugin/src/main/java/org/gradle/sample/plugins/toolingapi/custom/ToolingApiCustomModelPlugin.java)
  it is explained how to register your own model for Gradle Tooling API, so I did that. Surprisingly, it just works and
  requires no modifications to the analyzed project, which is a big win. At this point it seemed as if the hardest part
  was done
- (one thing that did not work is `includeBuild` directive in the init script (it is not aware of such a symbol). Of
  course, Gradle docs leave an impression that the settings file and the init script have the same functionality)
- (you might wonder why would I want to use a composite build, and the answer is simple - I did not like the
  publish-artifacts-and-then-use-them approach. However, I had to embrace it since there was no other option, haha)
- (another thing that did not work was `subprojects` closure, whereas `allprojects` worked)
- I thought to write some functional tests with various `build.gradle` contents, and adjust the plugin's code with the
  help of debugger. In order to have debugger access Tooling model's code I had to use `GradleConnector` in the plugin's
  code. Not an ideal approach, but I did not see any other option. For a "production" app that invocation could be
  guarded by a cmdline flag for instance (a `RUN_TOOLING_TEST` property actually), so it does not seem like a big issue
- sadly, my test build scripts would not work once I declared `dependsOn` relation. The same thing worked fine in the
  sample project, so I gave up, lol. The error was this:
  ```class org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet_Decorated cannot be cast to class org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet (org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet_Decorated is in unnamed module of loader org.gradle.internal.classloader.VisitableURLClassLoader$InstrumentingVisitableURLClassLoader @54c21d99; org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet is in unnamed module of loader org.gradle.internal.classloader.VisitableURLClassLoader$InstrumentingVisitableURLClassLoader @3c215aba)```
- I had one option left to try, it was to run the analyzer against a real project. I was able to create `dependsOn`
  declarations for project-local source sets, but not so much for inter-project source set dependencies. If I wrote
  `projects.untitled.commonMain`, IDEA would add an import like this...

```kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
```

- ...and tell me that it
  `Cannot access 'KotlinMultiplatformSourceSetConventionsImpl': it is internal in 'org. jetbrains. kotlin. gradle. dsl'`.
  It did not matter whether I used the type-safe project accessors or not. At this point I spent the same amount of time
  on configuring the "test" projects and on figuring out how to use Gradle Tooling API with a Gradle plugin (ostensibly,
  a more difficult and more interesting task)
- since I was left with local source set dependencies I focused on that. I tweaked the `OneSubproject` test file to load
  the real KMP project and added an init script to also load the plugin. Thankfully, this worked and I could use the
  debugger to see what `kotlin` extension provides
- I added a conditional breakpoint when `desktopMain` processing was happening, and finally could see a dependency on
  `commonMain2`:
  ![inspection](img/Screenshot%20at%202024-08-30%2011-00-58.png)
- whew! This is it, now need to arrange all this into a "hierarchy".
- got this error after adding `kotlin-gradle-plugin` to plugin's dependencies...:

```
* What went wrong:
Error resolving plugin [id: 'org.jetbrains.kotlin.multiplatform', version: '2.0.0']
> The request for this plugin could not be satisfied because the plugin is already on the classpath with an unknown version, so compatibility cannot be checked.
```

- ...I guess this is why reflection is used in JetBrain's projects. `compileOnly` builds successfully, but does not
  work at runtime. So reflection it is!
- I also noticed a `hierarchy` member on each project with KMP plugin, so the rest of the logic was built off of that.

# Fun facts

- Gradle "Tooling" API seems to be doing next to nothing. In order to get `Project` object with it we need to implement
  a Gradle plugin that will expose it via a custom wrapper class. The "Tooling" API will then serialize it and pass to
  its clients. I mean, if I have to write a plugin and attach it unobtrusively to a project just to use a "custom
  model", I might as well just register a task to print anything I want straight from the plugin.
- there is a `sourceSets` task, that prints what I understand as "main" source sets for each task. From the first glance
  it does not seem to give an insight into source sets' relations however, for instance:

```
debug
-----
Compile configuration: debugCompile
build.gradle name: android.sourceSets.debug
Java sources: [composeApp/src/debug/java]
Kotlin sources: [composeApp/src/androidDebug/kotlin, composeApp/build/generated/compose/resourceGenerator/kotlin/androidDebugResourceAccessors, composeApp/src/debug/java, composeApp/src/debug/kotlin]
Manifest file: composeApp/src/androidDebug/AndroidManifest.xml
Android resources: [composeApp/src/debug/res, composeApp/src/androidDebug/res]
Assets: [composeApp/src/debug/assets, composeApp/src/androidDebug/assets]
AIDL sources: [composeApp/src/debug/aidl, composeApp/src/androidDebug/aidl]
RenderScript sources: [composeApp/src/debug/rs, composeApp/src/androidDebug/rs]
Baseline profile sources: [composeApp/src/debug/baselineProfiles]
JNI sources: [composeApp/src/debug/jni, composeApp/src/androidDebug/jni]
JNI libraries: [composeApp/src/debug/jniLibs, composeApp/src/androidDebug/jniLibs]
Java-style resources: [composeApp/src/debug/resources, composeApp/src/androidDebug/resources, composeApp/build/kotlin-multiplatform-resources/assemble-hierarchically/androidDebugResources]
```

- Vampire [explains](https://youtrack.jetbrains.com/issue/IDEA-343338/Please-avoid-using-afterEvaluate-in-Gradle-logic)
  why `afterEvaluate` should not be the first choice
