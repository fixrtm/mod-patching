package com.anatawa12.modPatching.source.internal

import com.anatawa12.modPatching.source.internal.SourceConstants.MOD_PATCHING_SOURCE_UTIL_CLI_CONFIGURATION
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import java.io.File
import java.io.FileNotFoundException
import java.util.function.Consumer

fun File.readTextOr(ifNotFound: String = ""): String = try {
    readText()
} catch (e: FileNotFoundException) {
    ifNotFound
}

val Project.sourceSets get() = (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer

fun yamlReformat(project: Project): Consumer<File> {
    return Consumer { file ->
        val binary = project.configurations[MOD_PATCHING_SOURCE_UTIL_CLI_CONFIGURATION].singleFile
        binary.setExecutable(true)
        project.exec {
            isIgnoreExitValue = false
            executable = binary.path
            args("reformat-yaml", file.absolutePath)
        }
    }
}
