package com.anatawa12.modPatching.internal

import io.sigpipe.jbsdiff.DefaultDiffSettings
import io.sigpipe.jbsdiff.Diff
import java.io.File
import java.security.MessageDigest

object BsdiffPatchGenerator {
    fun generate(
        oldFiles: Map<String, File>,
        newFiles: Map<String, File>,
        outputDirectory: File,
        patchPrefix: String,
        compression: Compression,
    ) {
        check((newFiles.keys - oldFiles.keys).isEmpty()) { "some files are added: ${newFiles.keys - oldFiles.keys}" }
        val settings = DefaultDiffSettings<RuntimeException>(compression::apply)

        val patchDir = outputDirectory.resolve(patchPrefix)
        val sha1 = MessageDigest.getInstance("SHA-1")

        for ((newPath, newFile) in newFiles) {
            val oldFile = oldFiles[newPath] ?: error("logic failre: $newPath")

            val oldBytes = oldFile.readBytes()
            val newBytes = newFile.readBytes()

            val oldHashFile = patchDir.resolve("$newPath.old.sha1")
            oldHashFile.parentFile.mkdirs()
            oldHashFile.writeBytes(sha1.digest(oldBytes))

            if (oldBytes.contentEquals(newBytes))
                continue

            val bsDiffFile = patchDir.resolve("$newPath.bsdiff")
            val newHashFile = patchDir.resolve("$newPath.new.sha1")

            bsDiffFile.parentFile.mkdirs()
            newHashFile.parentFile.mkdirs()

            Diff.diff(oldBytes, newBytes, bsDiffFile.outputStream(), settings)
            newHashFile.writeBytes(sha1.digest(newBytes))
        }
    }
}
