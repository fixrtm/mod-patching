@file:JvmName("ApplyPatch")

package com.anatawa12.modPatching.internalTools

import com.anatawa12.modPatching.OnVCSPatchSource
import com.anatawa12.modPatching.internal.CommonConstants.PATCH_FILE_EXTENSION
import com.anatawa12.modPatching.internal.patchingDir.PatchingDir
import net.minecraftforge.gradle.util.patching.ContextualPatch
import java.io.File
import java.util.zip.ZipFile

fun main(args: Array<String>) {
    val patchingDir = PatchingDir.on(File("."))
    val mods = patchingDir.patchingMods.filter { it.onVcs == OnVCSPatchSource.PATCHES }

    for (mod in mods) {
        val raw = mod.sourceJarPath
        val patchesDir = mod.patchPath
        val src = mod.sourcePath

        val rawJar = ZipFile(raw)
        val patches = patchesDir.walkTopDown()
            .filter { it.isFile }
            .map { it.toRelativeString(patchesDir) }
            .toSet()

        mod.modifiedClasses.checkSame(
            patches.asSequence()
                .map { it.removeSuffix(".java.$PATCH_FILE_EXTENSION").replace('/', '.') },
            "those classes are not defined modified class",
            "patch for some class not found",
        )

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
                .writeText(ctx.dst.joinToString("\n"))
        }
    }
    patchingDir.flush()
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
