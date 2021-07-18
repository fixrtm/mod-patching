package com.anatawa12.modPatching.binary

import com.anatawa12.modPatching.binary.internal.flatten
import com.anatawa12.modPatching.internal.BsdiffPatchGenerator
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.io.File

open class GenerateBsdiffPatch : DefaultTask() {
    @InputFiles
    var oldFiles = project.objects.listProperty(FileTree::class).convention(emptyList())

    @InputFiles
    var newFiles = project.objects.listProperty(FileTree::class).convention(emptyList())

    @OutputDirectory
    val outTo = project.objects.property(File::class)

    @Input
    val patchPrefix = project.objects.property(String::class)

    @Input
    val compressionMethod = project.objects.property(String::class).convention("bzip2")

    @TaskAction
    fun run() {
        val oldFiles = getAllFiles(oldFiles.get().flatten(project))
        val newFiles = getAllFiles(newFiles.get().flatten(project))
        val outTo = outTo.get()
        val patchPrefix = patchPrefix.get()
        val compressionMethod = compressionMethod.get()

        val compressorFactory = CompressorStreamFactory()
        BsdiffPatchGenerator.generate(
            oldFiles,
            newFiles,
            outputDirectory = outTo,
            patchPrefix,
            compression = {
                compressorFactory.createCompressorOutputStream(compressionMethod, it)
            }
        )
    }

    private fun getAllFiles(fileTree: FileTree): Map<String, File> {
        val files = mutableMapOf<String, File>()
        fileTree.visit {
            if (isDirectory) return@visit
            files[path] = file
        }
        return files
    }
}
