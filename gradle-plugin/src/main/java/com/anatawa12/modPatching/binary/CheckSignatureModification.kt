package com.anatawa12.modPatching.binary

import com.anatawa12.modPatching.binary.internal.flatten
import com.anatawa12.modPatching.binary.internal.signatureCheck.SignatureChecker
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property

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
            check(
                base = baseClasses.get().flatten(project),
                modified = modifiedClasses.get().flatten(project),
                rootPackage = rootPackage.get(),
            )
            printDifferences(project.logger)
        }
    }
}
