package com.anatawa12.modPatching.binary

import com.anatawa12.modPatching.internal.SourceRenamer
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault
open class RenameSourceName : DefaultTask() {
    @Internal
    val classesDir = project.objects.property(File::class)

    @Input
    val suffix = project.objects.property(String::class)

    @TaskAction
    fun run() {
        SourceRenamer.rename(classesDir.get(), suffix.get())
    }
}
