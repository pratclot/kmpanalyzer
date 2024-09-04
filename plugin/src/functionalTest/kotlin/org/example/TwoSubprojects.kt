package org.example

import org.gradle.testkit.runner.GradleRunner
import org.gradle.tooling.GradleConnector
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class TwoSubprojects {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    private val buildFileSubproject1 by lazy {
        projectDir.run {
            resolve("subproject1").mkdir()
            resolve("subproject1/build.gradle")
        }
    }
    private val buildFileSubproject2 by lazy {
        projectDir.run {
            resolve("subproject2").mkdir()
            resolve("subproject2/build.gradle")
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
                id('org.example.greeting')
                id("org.jetbrains.kotlin.jvm") version "1.9.23"
            }
        """.trimIndent()
        )

        buildFileSubproject1.writeText(
            """
            plugins {
                id('org.example.greeting')
                id('org.jetbrains.kotlin.multiplatform')
            }
            
            kotlin {
                jvm("desktop")
            }
            
        """.trimIndent()
        )


        buildFileSubproject2.writeText(
            """
            plugins {
                id('org.example.greeting')
                id('org.jetbrains.kotlin.multiplatform')
            }
            
            kotlin {
                jvm("desktop")
            }
            
        """.trimIndent()
        )


        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("greeting")
        runner.withProjectDir(projectDir)
        runner.withDebug(true)
        val result = runner.build()

    }
}