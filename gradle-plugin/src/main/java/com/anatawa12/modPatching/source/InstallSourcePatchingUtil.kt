package com.anatawa12.modPatching.source

import com.anatawa12.modPatching.source.internal.OperatingSystem
import com.anatawa12.modPatching.source.internal.SourceConstants.MOD_PATCHING_SOURCE_UTIL_CLI_CONFIGURATION
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property

open class InstallSourcePatchingUtil : DefaultTask() {
    @Input
    val prefix = project.objects.property(String::class).convention("")

    @Internal
    val destination = project.objects.directoryProperty()

    @InputFiles
    protected val command = project.configurations.named(MOD_PATCHING_SOURCE_UTIL_CLI_CONFIGURATION)

    private fun filePath(dest: Directory, pre: String, command: String): RegularFile {
        return if (pre == "")
            dest.file("$command${OperatingSystem.current.extension}")
        else
            dest.file("$pre.$command${OperatingSystem.current.extension}")
    }

    @get:OutputFile
    protected val addModifyFile = destination
        .flatMap { dest -> prefix.map { pre -> filePath(dest, pre, "add-modify") } }

    @get:OutputFile
    protected val applyPatchesFile = destination
        .flatMap { dest -> prefix.map { pre -> filePath(dest, pre, "apply-patches") } }

    @get:OutputFile
    protected val createDiffFile = destination
        .flatMap { dest -> prefix.map { pre -> filePath(dest, pre, "create-diff") } }

    @TaskAction
    fun install() {
        val command = command.get().singleFile
        val addModifyFile = addModifyFile.get().asFile
        val applyPatchesFile = applyPatchesFile.get().asFile
        val createDiffFile = createDiffFile.get().asFile
        command.copyTo(addModifyFile, overwrite = true)
        command.copyTo(applyPatchesFile, overwrite = true)
        command.copyTo(createDiffFile, overwrite = true)
        addModifyFile.setExecutable(true)
        applyPatchesFile.setExecutable(true)
        createDiffFile.setExecutable(true)
    }
}
