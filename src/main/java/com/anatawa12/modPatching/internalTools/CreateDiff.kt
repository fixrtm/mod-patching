@file:JvmName("CreateDiff")

package com.anatawa12.modPatching.internalTools

import com.anatawa12.modPatching.OnVCSPatchSource
import com.anatawa12.modPatching.internal.CommonConstants.PATCH_FILE_EXTENSION
import com.anatawa12.modPatching.internal.patchingDir.PatchingDir
import com.cloudbees.diff.Diff
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile

fun main(args: Array<String>) {
    val patchingDir = PatchingDir.on(File("."))
    val mods = patchingDir.patchingMods.filter { it.onVcs == OnVCSPatchSource.PATCHES }

    for (mod in mods) {
        val raw = mod.sourceJarPath
        val patches = mod.patchPath
        val src = mod.sourcePath

        val rawJar = ZipFile(raw)
        val sources = src.walkTopDown()
            .filter { it.isFile }
            .map { it.toRelativeString(src) }
            .toSet()

        mod.modifiedClasses.checkSame(
            sources.asSequence()
                .map{ it.removeSuffix(".java").replace('/', '.') },
            "those classes are not defined modified class",
            "source for some class not found",
        )

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
    patchingDir.flush()
}
