package org.example

import java.io.Serializable


class DefaultModel(
    override val plugins: List<String>,
    override val sourceSets: List<String>,
    override val projectsWithSourceSets: List<Map<String, List<String>>>,
    override val projectName: String
) : Serializable, CustomModel {
    override fun hasPlugin(type: Class<*>?): Boolean {
        return plugins.contains(type?.name)
    }
}

interface CustomModel {
    val plugins: List<String>
    val sourceSets: List<String>
    val projectsWithSourceSets: List<Map<String, List<String>>>
    val projectName: String
    fun hasPlugin(type: Class<*>?): Boolean
}