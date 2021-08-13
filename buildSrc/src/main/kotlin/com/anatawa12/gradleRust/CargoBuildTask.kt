@file:Suppress("UnstableApiUsage")

package com.anatawa12.gradleRust

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import java.io.File

open class CargoBuildTask : DefaultTask(), EnvironmentProperties {
    private val plugin = project.plugins.getPlugin(GradlePlugin::class)

    init {
        // always run this task because cargo has very intelligent build caching system
        outputs.upToDateWhen { false }
    }

    /**
     * path to cargo command.
     */
    @Input
    val toolChain = project.objects.property(CargoToolChain::class).convention(plugin.extension.default)

    /**
     * Compile target.
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

    @OutputDirectory
    val targetDirectory = project.provider { destinationDir.dir(target) }
        .flatMap { it }

    @get:Internal
    val targetName = project.objects.property(String::class).convention(projectDir.map { it.asFile.name })

    @get:Internal
    val targetFileName: String
        get() {
            val (prefix, suffix) = toolChain.get().getDestinationFileType(target.get(), "bin")
            return prefix + targetName.get() + suffix
        }

    /**
     * path to the destination file
     */
    @get:OutputFile
    val binaryFile: Provider<RegularFile> = project.objects.fileProperty()
        .convention(targetDirectory.map { it.file((if (releaseBuild.get()) "release/" else "debug/") + targetFileName) })

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
            args("build")
            args("--target", target.get())
            if (releaseBuild.get())
                args("--release")
            args("--target-dir", destinationDir)

            args("--manifest-path", manifestPath)
        }
    }
}
