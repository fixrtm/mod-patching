package com.anatawa12.modPatching.internal

import com.anatawa12.modPatching.common.internal.CommonUtil
import net.minecraftforge.gradle.user.patcherUser.forge.ForgeExtension
import net.minecraftforge.gradle.user.patcherUser.forge.ForgePlugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSetContainer
import java.io.File

object Util {
    fun getBuildPath(project: Project, vararg pathElements: String): File {
        var cache = project.buildDir
        cache = cache.resolve("patching-mod")
        for (pathElement in pathElements) {
            cache = cache.resolve(CommonUtil.escapePathElement(pathElement))
        }
        return cache
    }
}

val Project.minecraft get() = (this as ExtensionAware).extensions.getByName("minecraft") as ForgeExtension

val Project.forgePlugin get() = project.plugins.getPlugin(ForgePlugin::class.java)

val Project.sourceSets get() = (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer

fun Iterable<FileTree>.flatten(project: Project): FileTree {
    var tree = project.files().asFileTree
    forEach { tree += it }
    return tree
}
