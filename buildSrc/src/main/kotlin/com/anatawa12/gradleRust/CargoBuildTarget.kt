package com.anatawa12.gradleRust

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import org.gradle.util.GUtil

class CargoBuildTarget(
    project: Project,
    cargoProject: CargoProject,
    private val targetTriple: String,
) : Named, EnvironmentProperties by EnvironmentPropertiesContainer() {
    val toolChain = project.objects.property(CargoToolChain::class).convention(cargoProject.toolChain)
    val projectDir = project.objects.directoryProperty().convention(cargoProject.projectDir)
    val destinationDir = project.objects.directoryProperty().convention(cargoProject.destinationDir)
    val targetName = project.objects.property(String::class).convention(cargoProject.targetName)
    val releaseBuild = project.objects.property(Boolean::class).convention(cargoProject.releaseBuild)
    val dependencyTasks = project.objects.setProperty(Task::class).convention(cargoProject.dependencyTasks)

    val build: CargoBuildTask
    val test: CargoTestTask

    init {
        extendsFrom(cargoProject)
        val buildTarget = this@CargoBuildTarget
        build = project.tasks.create(buildTaskName("build", cargoProject, targetTriple), CargoBuildTask::class) {
            target.set(targetTriple)
            toolChain.set(buildTarget.toolChain)
            projectDir.set(buildTarget.projectDir)
            destinationDir.set(buildTarget.destinationDir)
            targetName.set(buildTarget.targetName)
            releaseBuild.set(buildTarget.releaseBuild)
            cargoProject.buildTask.dependsOn(this)
            dependsOn(buildTarget.dependencyTasks)
            extendsFrom(buildTarget)
        }
        test = project.tasks.create(buildTaskName("test", cargoProject, targetTriple), CargoTestTask::class) {
            target.set(targetTriple)
            toolChain.set(buildTarget.toolChain)
            projectDir.set(buildTarget.projectDir)
            destinationDir.set(buildTarget.destinationDir)
            releaseBuild.set(buildTarget.releaseBuild)
            cargoProject.testTask.dependsOn(this)
            dependsOn(buildTarget.dependencyTasks)
            extendsFrom(buildTarget)
        }
    }

    // built value information
    val binaryFile: Provider<RegularFile> = build.binaryFile

    fun build(block: Action<CargoBuildTask>) {
        block.execute(build)
    }

    fun build(block: Closure<*>) {
        build.configure(block)
    }

    fun test(block: Action<CargoTestTask>) {
        block.execute(test)
    }

    fun test(block: Closure<*>) {
        test.configure(block)
    }

    fun disableTesting() {
        test.enabled = false
    }

    fun enableTesting() {
        test.enabled = false
    }

    companion object {
        private fun buildTaskName(verb: String, project: CargoProject, targetName: String): String =
            GUtil.toLowerCamelCase("$verb cargo ${project.name} for ${targetName.replace('-', ' ')}")
    }

    override fun getName(): String = targetTriple
}
