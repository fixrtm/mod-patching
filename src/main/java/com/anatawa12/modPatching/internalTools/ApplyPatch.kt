@file:JvmName("ApplyPatch")

package com.anatawa12.modPatching.internalTools

import com.anatawa12.modPatching.OnVCSPatchSource
import com.anatawa12.modPatching.internal.CommonConstants
import com.anatawa12.modPatching.internal.CommonConstants.PATCH_FILE_EXTENSION
import com.anatawa12.modPatching.internal.unescapeStringForFile
import com.cloudbees.diff.Diff
import net.minecraftforge.gradle.util.patching.ContextualPatch
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile

fun main(args: Array<String>) {
    data class PatchesModInfo(
        val rawSrc: File,
        val src: File,
        val patches: File,
        val modifieds: Set<String>,
    )

    val patchingDir = File("./${CommonConstants.PATCHING_DIR_NAME}")
    val mods = patchingDir.listFiles()
        ?.asSequence().let { it ?: emptySequence() }
        .filter { it.name.endsWith(".${CommonConstants.MOD_DIR_EXTENSION}") }
        .filter { it.resolve(CommonConstants.MOD_ON_VCS_CONFIG_FILE_NAME).readText().trim() == OnVCSPatchSource.PATCHES.name }
        .map { modDir ->
            PatchesModInfo(
                File(modDir.resolve(CommonConstants.SOURCE_JAR_PATH_CONFIG_FILE_NAME)
                    .readText().lineSequence().first().unescapeStringForFile()),
                File(modDir.resolve(CommonConstants.SOURCE_DIR_PATH_CONFIG_FILE_NAME)
                    .readText().lineSequence().first().unescapeStringForFile()),
                File(modDir.resolve(CommonConstants.PATCH_DIR_PATH_CONFIG_FILE_NAME)
                    .readText().lineSequence().first().unescapeStringForFile()),
                modDir.resolve(CommonConstants.MODIFIED_CLASSES_CONFIG_FILE_NAME)
                    .readText().lineSequence().map { it.unescapeStringForFile() }
                    .filterNot { it.isBlank() }.toSet(),
            )
        }

    for ((raw, src, patchesDir, modifieds) in mods) {
        val rawJar = ZipFile(raw)
        val patches = patchesDir.walkTopDown()
            .filter { it.isFile }
            .map { it.toRelativeString(patchesDir) }
            .toSet()
        val modifiedsCopy = modifieds.toMutableSet()
        for (patch in patches) {
            val patchClass = patch.removeSuffix(".java.$PATCH_FILE_EXTENSION")
                .replace('/', '.')
            if (patchClass !in modifiedsCopy) error("$patchClass is not defined modified class")
            modifiedsCopy.remove(patchClass)
        }
        if (modifiedsCopy.isNotEmpty()) error("patch for some class not found: \n" +
                modifiedsCopy.joinToString("\n"))
        for (patch in patches) {
            val javaName = patch.removeSuffix(".$PATCH_FILE_EXTENSION")
            if (rawJar.getEntry(javaName) == null)
                error("$javaName not found in original source")
        }

        src.deleteRecursively()
        for (patch in patches) {
            val javaName = patch.removeSuffix(".$PATCH_FILE_EXTENSION")
            val srcPath = src.resolve(javaName)

            val patchSrc = patchesDir.resolve(patch).readText()
            val rawSrc = rawJar.getInputStream(rawJar.getEntry(javaName)).reader().use { it.readText() }

            val ctx = ContextProviderImpl("a/$srcPath", rawSrc.lines())
            ContextualPatch.create(patchSrc, ctx)
                .setMaxFuzz(2)
                .patch(false)
            src.resolve(javaName)
                .apply { parentFile.mkdirs() }
                .writeText(ctx.dst.joinToString("") { "$it\n" })
        }
    }
}

private class ContextProviderImpl(val target: String, val src: List<String>): ContextualPatch.IContextProvider {
    lateinit var dst: List<String>
    override fun getData(target: String): List<String> {
        check(this.target != target) { "invalid target: $target" }
        return src
    }

    override fun setData(target: String, data: List<String>) {
        dst = data
    }
}
