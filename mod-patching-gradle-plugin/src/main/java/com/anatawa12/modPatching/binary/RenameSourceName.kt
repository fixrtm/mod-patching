package com.anatawa12.modPatching.binary

import com.anatawa12.modPatching.internal.SourceRenamer
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import java.io.File

open class RenameSourceName : DefaultTask() {
    @OutputDirectory
    val classesDir = project.objects.property(File::class)
    @Input
    val suffix = project.objects.property(String::class)

    @TaskAction
    fun run() {
        SourceRenamer.rename(classesDir.get(), suffix.get())
    }
}
