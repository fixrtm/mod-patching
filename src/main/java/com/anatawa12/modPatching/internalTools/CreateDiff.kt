@file:JvmName("CreateDiff")

package com.anatawa12.modPatching.internalTools

import com.anatawa12.modPatching.OnVCSPatchSource
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
import com.anatawa12.modPatching.internal.unescapeStringForFile
import com.cloudbees.diff.Diff
import org.gradle.internal.impldep.org.eclipse.jgit.api.Git
import java.io.File
import java.io.StringReader
import java.util.*
import java.util.zip.ZipFile

fun main(args: Array<String>) {
    data class PatchesModInfo(
        val rawSrc: File,
        val src: File,
        val patches: File,
        val modifieds: Set<String>,
    )

    val patchingDir = File("./$PATCHING_DIR_NAME")
    val mods = patchingDir.listFiles()
        ?.asSequence().let { it ?: emptySequence() }
        .filter { it.name.endsWith(".$MOD_DIR_EXTENSION") }
        .filter { it.resolve(MOD_ON_VCS_CONFIG_FILE_NAME).readText().trim() == OnVCSPatchSource.PATCHES.name }
        .map { modDir ->
            PatchesModInfo(
                File(modDir.resolve(SOURCE_JAR_PATH_CONFIG_FILE_NAME)
                    .readText().lineSequence().first().unescapeStringForFile()),
                File(modDir.resolve(SOURCE_DIR_PATH_CONFIG_FILE_NAME)
                    .readText().lineSequence().first().unescapeStringForFile()),
                File(modDir.resolve(PATCH_DIR_PATH_CONFIG_FILE_NAME)
                    .readText().lineSequence().first().unescapeStringForFile()),
                modDir.resolve(MODIFIED_CLASSES_CONFIG_FILE_NAME)
                    .readText().lineSequence().map { it.unescapeStringForFile() }
                    .filterNot { it.isBlank() }.toSet(),
            )
        }

    for ((raw, src, patches, modifieds) in mods) {
        val rawJar = ZipFile(raw)
        val sources = src.walkTopDown()
            .filter { it.isFile }
            .map { it.toRelativeString(src) }
            .toSet()
        val modifiedsCopy = modifieds.toMutableSet()
        for (source in sources) {
            val sourceClass = source.removeSuffix(".java").replace('/', '.')
            if (sourceClass !in modifiedsCopy) error("$sourceClass is not defined modified class")
            modifiedsCopy.remove(sourceClass)
        }
        if (modifiedsCopy.isNotEmpty()) error("source file for some class not found: \n" +
                modifiedsCopy.joinToString("\n"))
        for (source in sources) {
            if (rawJar.getEntry(source) == null)
                error("$source not found in original source")
        }

        patches.deleteRecursively()
        for (source in sources) {
            val rawSrc = rawJar.getInputStream(rawJar.getEntry(source)).reader().use { it.readText() }
            val srcSrc = src.resolve(source).readText()
            val diff = Diff.diff(rawSrc.lines(), srcSrc.lines(), false)
            val unifiedDiff = diff.toUnifiedDiff(
                "a/$source",
                "b/$source",
                StringReader(rawSrc),
                StringReader(srcSrc),
                5,
            )
            patches.resolve("$source.$PATCH_FILE_EXTENSION")
                .apply { parentFile.mkdirs() }
                .writeText(unifiedDiff)
        }

        GitWrapper.add(patches)
    }
}
