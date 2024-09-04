initscript {
    repositories {
        mavenCentral()
        mavenLocal()
    }
    dependencies {
        classpath("org.example.greeting:org.example.greeting.gradle.plugin:1.0.0")
    }
}

allprojects {
    apply<org.example.PluginPlugin>()
}