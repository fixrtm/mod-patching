package com.anatawa12.modPatching.source

import com.anatawa12.modPatching.internal.DeObfuscator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import java.io.File

open class DeobfuscateSrg : DefaultTask() {
    @InputFiles
    val mappings = project.objects.fileCollection()

    @InputFile
    val sourceJar = project.objects.property(File::class)

    @OutputFile
    val destination = project.objects.property(File::class)

    @TaskAction
    fun deobfucate() {
        val sourceJar = sourceJar.get()
        val destination = destination.get()

        DeObfuscator.deobfucate(
            sourceJar,
            destination,
            mappings.singleFile,
        )
    }
}
