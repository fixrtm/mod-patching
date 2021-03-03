@file:JvmName("AddModifyFiles")

package com.anatawa12.modPatching.internalTools

import com.anatawa12.modPatching.OnRepoPatchSource
import com.anatawa12.modPatching.OnVCSPatchSource
import com.anatawa12.modPatching.internal.CommonConstants.PATCH_FILE_EXTENSION
import com.anatawa12.modPatching.internal.indexOfFirst
import com.anatawa12.modPatching.internal.patchingDir.PatchingDir
import java.io.File
import java.util.*
import java.util.zip.ZipFile

fun main(args: Array<String>) {
    val sc = Scanner(System.`in`)
    val matcher = args.getOrNull(0) ?: error("class name not found")
    val classFileNameMatcher = makeJavaFileNameMatcher(matcher)

    val patchingDir = PatchingDir.on(File("."))
    val (mod, entry) = patchingDir.patchingMods.asSequence()
        .filter { it.onRepo == OnRepoPatchSource.MODIFIED }
        .flatMap { mod ->
            ZipFile(mod.sourceJarPath).use { it.entries().toList() }
                .asSequence()
                .filter { classFileNameMatcher(it.name) }
                .map { mod to it }
        }
        .chooseOne(
            5, "classes", sc,
            { it.second.name },
            { "no class matches '$matcher' not found" },
            { "more than 5 classes matches '$matcher' found" },
        )

    val javaFilePath = mod.sourcePath.resolve(entry.name)
    if (javaFilePath.exists()) return println("${entry.name} already exists")

    javaFilePath.parentFile.mkdirs()
    ZipFile(mod.sourceJarPath).use { it.getInputStream(entry).copyTo(javaFilePath.outputStream()) }
    mod.modifiedClasses += entry.name.removeSuffix(".java").replace('/', '.')
    if (mod.onVcs == OnVCSPatchSource.PATCHES) {
        val patchFile = mod.patchPath.resolve("${entry.name}.${PATCH_FILE_EXTENSION}")
        patchFile.parentFile.mkdirs()
        patchFile.writeText("""
                --- a/${entry.name}
                +++ b/${entry.name}
            """.trimIndent())
        GitWrapper.add(patchFile)
    }
    patchingDir.flush()
}
