@file:JvmName("AddModifyFiles")

package com.anatawa12.modPatching.internalTools

import com.anatawa12.modPatching.internal.CommonConstants.MODIFIED_CLASSES_CONFIG_FILE_NAME
import com.anatawa12.modPatching.internal.CommonConstants.MOD_DIR_EXTENSION
import com.anatawa12.modPatching.internal.CommonConstants.MOD_ON_REPO_CONFIG_FILE_NAME
import com.anatawa12.modPatching.internal.CommonConstants.MOD_ON_VCS_CONFIG_FILE_NAME
import com.anatawa12.modPatching.internal.CommonConstants.PATCHING_DIR_NAME
import com.anatawa12.modPatching.internal.CommonConstants.PATCH_DIR_PATH_CONFIG_FILE_NAME
import com.anatawa12.modPatching.internal.CommonConstants.PATCH_FILE_EXTENSION
import com.anatawa12.modPatching.internal.CommonConstants.SOURCE_DIR_PATH_CONFIG_FILE_NAME
import com.anatawa12.modPatching.internal.CommonConstants.SOURCE_JAR_PATH_CONFIG_FILE_NAME
import com.anatawa12.modPatching.internal.escapeStringForFile
import com.anatawa12.modPatching.internal.indexOfFirst
import com.anatawa12.modPatching.internal.unescapeStringForFile
import java.io.File
import java.util.*
import java.util.zip.ZipFile

fun main(args: Array<String>) {
    val sc = Scanner(System.`in`)
    val matcher = args.getOrNull(0) ?: error("class name not found")
    val classFileNameMatcher = makeJavaFileNameMatcher(matcher)

    val patchingDir = File("./$PATCHING_DIR_NAME")
    val mods = patchingDir.listFiles()
        ?.asSequence().let { it ?: emptySequence() }
        .filter { it.name.endsWith(".$MOD_DIR_EXTENSION") }
        .filter { it.resolve(MOD_ON_REPO_CONFIG_FILE_NAME).readText().trim() == "MODIFIED" }
        .map { it to File(it.resolve(SOURCE_JAR_PATH_CONFIG_FILE_NAME).readText().unescapeStringForFile()) }
        .map { it.first to ZipFile(it.second) }
        .toList()
        .flatMap { (dir, mod) ->
            val entries = mod.entries().toList()
                .filter { classFileNameMatcher(it.name) }
                .map { Triple(dir, mod, it) }
            if (entries.isEmpty()) mod.close()
            entries
        }
    if (mods.isEmpty()) error("no class matches '$matcher' not found")
    if (mods.size > 5) error("more than 5 classes matches '$matcher' found")
    val (dir, jar, entry) = if (mods.size == 1) mods[0]
    else {
        println("many classes found. choose from list below")
        for ((i, file) in mods.withIndex()) {
            println("$i: ${file.third.name}")
        }
        print("index:")
        System.out.flush()
        val index = sc.nextInt()
        if (index !in mods.indices)
            error("invalid index: $index")
        val useMod = mods[index]
        for ((_, jar, _) in mods) if (useMod.second != jar) jar.close()
        useMod
    }
    val sourceDir = File(dir.resolve(SOURCE_DIR_PATH_CONFIG_FILE_NAME).readText().unescapeStringForFile())
    val javaFilePath = sourceDir.resolve(entry.name)
    if (javaFilePath.exists()) return println("${entry.name} already exists")
    javaFilePath.parentFile.mkdirs()
    jar.getInputStream(entry).copyTo(javaFilePath.outputStream())
    dir.resolve(MODIFIED_CLASSES_CONFIG_FILE_NAME)
        .appendText("${entry.name.removeSuffix(".java").replace('/', '.').escapeStringForFile()}\n")
    if (dir.resolve(MOD_ON_VCS_CONFIG_FILE_NAME).readText() == "PATCHES") {
        val patchDir = File(dir.resolve(PATCH_DIR_PATH_CONFIG_FILE_NAME).readText().unescapeStringForFile())
        val patchFile = patchDir.resolve("${entry.name}.${PATCH_FILE_EXTENSION}")
        patchFile.parentFile.mkdirs()
        patchFile.writeText("""
                --- a/${entry.name}
                +++ b/${entry.name}
            """.trimIndent())
        GitWrapper.add(patchFile)
    }
}

fun makeJavaFileNameMatcher(matcher: String): (String) -> Boolean {
    val elements = matcher.split('.', '/', '\\')
    check (elements.all { it.isNotEmpty() }) { "some class name element is empty" }
    return matcher@{ fileName ->
        if (!fileName.endsWith(".java")) return@matcher false
        val name = fileName.removeSuffix(".java")
        var matchIndex = -1
        val fileNameElements = name.split('/')
        for (element in elements) {
            matchIndex = fileNameElements.indexOfFirst(matchIndex + 1) { it.contains(elements[0]) }
            if (matchIndex == -1) return@matcher false
        }
        true
    }
}
