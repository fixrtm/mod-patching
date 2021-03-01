package com.anatawa12.modPatching.internal

import java.io.File

fun String.unescapeStringForFile(): String {
    val baskSlashIndex = indexOf('\\')
    if (baskSlashIndex < 0) return this
    return buildString {
        append(substring(0, baskSlashIndex))
        var afterBaskSlash = true
        for (i in baskSlashIndex + 1 until length) {
            if (afterBaskSlash) when (this[i]) {
                'n' -> append('\n')
                'r' -> append('\r')
                '\\' -> append('\\')
                else -> error("unexpect char after \\: '${this[i]}'")
            } else if (this[i] == '\\') {
                afterBaskSlash = true
            } else {
                append(this[i])
            }
        }
    }
}

fun File.escapePathStringForFile(): String = path.escapePathStringForFile()
fun String.escapePathStringForFile(): String {
    val escapedNeedIndex = indexOfAny(escapeNeededChars)
    if (escapedNeedIndex < 0) return this
    return buildString {
        append(substring(0, escapedNeedIndex))
        for (i in escapedNeedIndex until length) {
            when (val c = this[i]) {
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\\' -> append('/')
                else -> append(c)
            }
        }
    }
}

fun String.escapeStringForFile(): String {
    val escapedNeedIndex = indexOfAny(escapeNeededChars)
    if (escapedNeedIndex < 0) return this
    return buildString {
        append(substring(0, escapedNeedIndex))
        for (i in escapedNeedIndex until length) {
            when (val c = this[i]) {
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\\' -> append("\\\\")
                else -> append(c)
            }
        }
    }
}

private val escapeNeededChars = "\\\r\n".toCharArray()
