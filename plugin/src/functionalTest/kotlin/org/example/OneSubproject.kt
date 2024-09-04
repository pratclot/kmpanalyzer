package org.example

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class OneSubproject {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    private val buildFileSubproject by lazy {
        projectDir.run {
            resolve("subproject").mkdir()
            resolve("subproject/build.gradle.kts")
        }
    }

    @Test
    fun `can detect java plugin in a subproject`() {
        // Set up the test build
        settingsFile.writeText(
            """
            pluginManagement {
                repositories {
                    mavenCentral()
                    google()
                }
            }
                
            include('subproject')
//            includeBuild('${System.getProperty("user.dir")}/../plugin')
        """.trimIndent()
        )
        buildFile.writeText(
            """
            plugins {
//                id('org.example.greeting') apply false
                id('org.example.greeting')
                id("org.jetbrains.kotlin.jvm") version "2.0.0" apply false
            }
//            plugins {
//                id('org.example.greeting')
////                id("org.jetbrains.kotlin.jvm")
//            }
        """.trimIndent()
        )

        buildFileSubproject.writeText(
            """
            plugins {
                id("org.example.greeting")
                id("org.jetbrains.kotlin.multiplatform")
            }
            
            kotlin {
                jvm("desktop")
                
                sourceSets {
//                None of this works apparently
//                    val commonMain2 by creating
                    val commonMain2 by creating {
                        dependsOn(commonMain.get())
                    }
//                    commonMain2.dependsOn(commonMain)
//                    commonMain2.get().dependsOn(commonMain)
//                    commonMain2.get().dependsOn(commonMain.get())

                    val desktopMain by getting
                    desktopMain.dependsOn(commonMain2)
                }
            }
            
        """.trimIndent()
        )


        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("help")
//        runner.withProjectDir(projectDir)
//        Let's test in prod!
        runner.withArguments(
            "help",
            "--init-script",
            "${System.getProperty("user.dir")}/../initScriptForActualProject.gradle.kts",
            "--stacktrace",
            "-PRUN_TOOLING_TEST"
        )
        runner.withProjectDir(File("${System.getProperty("user.dir")}/../../KotlinProject"))
        runner.withDebug(true)
        val result = runner.build()

    }
}