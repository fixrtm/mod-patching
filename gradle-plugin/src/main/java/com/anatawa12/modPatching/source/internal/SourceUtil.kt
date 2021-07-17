package com.anatawa12.modPatching.source.internal

import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSetContainer
import java.io.File
import java.io.FileNotFoundException

fun File.readTextOr(ifNotFound: String = ""): String = try {
    readText()
} catch (e: FileNotFoundException) {
    ifNotFound
}

val Project.forgePlugin get() = project.plugins.getPlugin(ForgePlugin::class.java)

val Project.sourceSets get() = (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer
