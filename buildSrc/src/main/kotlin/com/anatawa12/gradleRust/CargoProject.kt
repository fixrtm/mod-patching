@file:Suppress("UnstableApiUsage")

package com.anatawa12.gradleRust

import groovy.lang.Closure
import org.gradle.api.*
import org.gradle.kotlin.dsl.*
import org.gradle.util.GUtil

class CargoProject(
    private val name: String,
    private val project: Project,
    extension: CargoExtension,
) : Named, EnvironmentProperties by EnvironmentPropertiesContainer() {
    override fun getName(): String = name
    val toolChain = project.objects.property(CargoToolChain::class).convention(extension.default)

    /**
     * the set of tasks which cargo build task will depend on
     */
    val dependencyTasks = project.objects.setProperty(Task::class)

    /**
     * path to project
     */
    val projectDir = project.objects.directoryProperty()
        .apply { finalizeValueOnRead() }

    /**
     * is this build release build. defaults true.
     */
    val releaseBuild = project.objects.property(Boolean::class).convention(true)
        .apply { finalizeValueOnRead() }

    /**
     * path to output directory
     */
    val destinationDir = project.objects.directoryProperty()
        .convention(project.layout.buildDirectory.map { it.dir("cargo/") })

    val targetName = project.objects.property(String::class).convention(projectDir.map { it.asFile.name })

    val buildTask = project.tasks.create(GUtil.toLowerCamelCase("build cargo $name"), Task::class) {
        project.tasks["assemble"].dependsOn(this)
    }

    val targets = project.objects.domainObjectContainer(CargoBuildTask::class) {
        project.tasks.create(buildTaskName(it), CargoBuildTask::class) {
            val project = this@CargoProject
            target.set(it)
            toolChain.set(project.toolChain)
            projectDir.set(project.projectDir)
            destinationDir.set(project.destinationDir)
            targetName.set(project.targetName)
            releaseBuild.set(project.releaseBuild)
            buildTask.dependsOn(this)
            extendsFrom(project)
            dependsOn(dependencyTasks)
        }
    }

    private fun buildTaskName(targetName: String): String =
        GUtil.toLowerCamelCase("build cargo $name for ${targetName.replace('-', ' ')}")

    fun targets(block: Action<NamedDomainObjectContainer<CargoBuildTask>>) {
        block.execute(targets)
    }

    fun targets(block: Closure<*>) {
        targets.configure(block)
    }
}
