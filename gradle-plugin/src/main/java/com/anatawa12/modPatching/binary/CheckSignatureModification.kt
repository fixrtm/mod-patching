package com.anatawa12.modPatching.binary

import com.anatawa12.modPatching.binary.internal.flatten
import com.anatawa12.modPatching.internal.SignatureChecker
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.util.function.Supplier

open class CheckSignatureModification : DefaultTask() {
    @InputFiles
    val baseClasses = project.objects.listProperty<FileTree>()

    @InputFiles
    val modifiedClasses = project.objects.listProperty<FileTree>()

    @InputFiles
    val rootPackage = project.objects.property<String>().convention("")

    @TaskAction
    open fun run() {
        SignatureChecker().apply {
            val base = baseClasses.get().flatten(project).toMap()
            val modified = modifiedClasses.get().flatten(project).toMap()
            check(
                base = base.asSequence()::iterator,
                modifiedGetter = { modified[it]?.get() },
                rootPackage = rootPackage.get(),
            )
            printDifferences(project.logger)
        }
    }

    private fun FileTree.toMap(): Map<String, Supplier<ByteArray>> {
        val files = mutableMapOf<String, Supplier<ByteArray>>()
        visit { files[path] = Supplier { file.readBytes() } }
        return files
    }
}
