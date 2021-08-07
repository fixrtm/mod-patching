package com.anatawa12.modPatching.source

import com.anatawa12.modPatching.source.internal.OperatingSystem
import com.anatawa12.modPatching.source.internal.SourceConstants.MOD_PATCHING_SOURCE_UTIL_CLI_CONFIGURATION
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import java.io.File
import java.nio.file.Files

open class InstallSourcePatchingUtil : DefaultTask() {
    @Input
    val installingType = project.objects.property(InstallingType::class)
        .convention(project.provider { InstallingType.default })

    @Input
    val prefix = project.objects.property(String::class).convention("")

    @Internal
    val destination = project.objects.directoryProperty()

    @InputFiles
    protected val command = project.configurations.named(MOD_PATCHING_SOURCE_UTIL_CLI_CONFIGURATION)

    private fun filePath(dest: Directory, pre: String, command: String): RegularFile {
        return if (pre == "")
            dest.file("$command${OperatingSystem.current!!.extension}")
        else
            dest.file("$pre.$command${OperatingSystem.current!!.extension}")
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
        val command = command.get().singleFile.absoluteFile
        val addModifyFile = addModifyFile.get().asFile.absoluteFile
        val applyPatchesFile = applyPatchesFile.get().asFile.absoluteFile
        val createDiffFile = createDiffFile.get().asFile.absoluteFile
        val installer: (File, File) -> Unit = when (installingType.get()) {
            InstallingType.Symlink -> ::installViaSymlink
            InstallingType.Copying -> ::installViaCopy
        }
        installer(command, addModifyFile)
        installer(command, applyPatchesFile)
        installer(command, createDiffFile)
        addModifyFile.setExecutable(true)
        applyPatchesFile.setExecutable(true)
        createDiffFile.setExecutable(true)
    }

    private fun installViaSymlink(from: File, to: File) {
        Files.createSymbolicLink(from.toPath(), to.toPath())
    }

    private fun installViaCopy(from: File, to: File) {
        from.copyTo(to, overwrite = true)
    }
}
