package com.anatawa12.gradleRust

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.kotlin.dsl.create

class GradlePlugin : Plugin<Project> {
    lateinit var extension: CargoExtension
        private set

    override fun apply(project: Project) {
        project.plugins.apply(BasePlugin::class.java)
        extension = project.extensions.create("cargo", project)
    }
}
