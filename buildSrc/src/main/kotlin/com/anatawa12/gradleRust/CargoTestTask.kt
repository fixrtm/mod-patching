@file:Suppress("UnstableApiUsage")

package com.anatawa12.gradleRust

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getPlugin
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault
open class CargoTestTask : DefaultTask(), EnvironmentProperties {
    private val plugin = project.plugins.getPlugin(GradlePlugin::class)

    /**
     * path to cargo command.
     */
    @Input
    val toolChain = project.objects.property(CargoToolChain::class).convention(plugin.extension.default)

    /**
     * Compile / run target.
     */
    @Input
    val target = project.objects.property(String::class).convention(toolChain.map { it.getDefaultTarget() })

    /**
     * path to project
     */
    // due to gradle's optimization
    @get:Internal
    val projectDir = project.objects.directoryProperty()

    /**
     * is this build release build. defaults true.
     */
    @Input
    val releaseBuild = project.objects.property(Boolean::class).convention(true)

    /**
     * path to output directory
     */
    @get:Internal
    val destinationDir = project.objects.directoryProperty()
        .convention(project.layout.buildDirectory
            .map { it.dir("cargo/") })

    private val container = EnvironmentPropertiesContainer()

    @get:Internal
    override val environment
        get() = container.environment

    @get:Input
    override val allEnvironment
        get() = container.allEnvironment

    override fun environment(environmentVariables: Map<String, *>) = container.environment(environmentVariables)
    override fun environment(name: String, value: Any?) = container.environment(name, value)
    override fun extendsFrom(parent: EnvironmentProperties) = container.extendsFrom(parent)

    @TaskAction
    fun runCargo() {
        val workdir: File
        val manifestPath: Any
        val projectDir = projectDir.get()
        workdir = projectDir.asFile
        manifestPath = "Cargo.toml"
        val destinationDir = destinationDir.get().asFile

        project.exec {
            isIgnoreExitValue = false

            workingDir(workdir)

            environment(allEnvironment)

            executable = toolChain.get().cargo
            args("test")
            args("--target", target.get())
            if (releaseBuild.get())
                args("--release")
            args("--target-dir", destinationDir)

            args("--manifest-path", manifestPath)
        }
    }
}
