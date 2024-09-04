/*
 * This source file was generated by the Gradle 'init' task
 */
package org.example

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * A simple unit test for the 'org.example.greeting' plugin.
 */
class PluginPluginTest {
    @Test fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.example.greeting")

        // Verify the result
        assertNotNull(project.tasks.findByName("greeting"))
    }
}
