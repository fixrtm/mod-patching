package com.anatawa12.modPatching.common.internal

import okhttp3.internal.toHexString
import org.gradle.api.Project
import java.io.File

object CommonUtil {
    fun getCachePath(project: Project, vararg pathElements: String): File {
        var cache = project.gradle.gradleUserHomeDir
        cache = cache.resolve("caches/minecraft-mods")
        for (pathElement in pathElements) {
            cache = cache.resolve(escapePathElement(pathElement))
        }
        return cache
    }

    fun escapePathElement(pathElement: String): String {
        for ((i, c) in pathElement.withIndex()) {
            if (c < 128.toChar()) continue
            if (c == '_') return doEscapePathElement(pathElement, i)
            if (c in FILESYSTEM_DISALLOWED_CHARS) return doEscapePathElement(pathElement, i)
            if (c < CHAR_32) return doEscapePathElement(pathElement, i)
        }

        return when(pathElement) {
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
            -> "_Z$pathElement"
            else -> pathElement
        }
    }

    private fun doEscapePathElement(pathElement: String, start: Int): String = buildString {
        append(pathElement, 0, start)
        for (i in start until pathElement.length) {
            val c = pathElement[i]
            when {
                c == '_' -> append("__")
                c < CHAR_32 -> append(ESCAPED_UNTIL_32[i])
                c in FILESYSTEM_DISALLOWED_CHARS -> append(ESCAPED_DISALLOWED[i])
                else -> append(c)
            }
        }
    }

    // convert to _G.._O
    private const val FILESYSTEM_DISALLOWED_CHARS = "<>:\"/\\|?*"
    private const val CHAR_32 = ' '
    private val ESCAPED_UNTIL_32 = Array(32) { "_${it.toHexString().padStart(2, '0')}" }
    private val ESCAPED_DISALLOWED = Array(FILESYSTEM_DISALLOWED_CHARS.length) { "_${(it + 'G'.toInt()).toChar()}" }
}
