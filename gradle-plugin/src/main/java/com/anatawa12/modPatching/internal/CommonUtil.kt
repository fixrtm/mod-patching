package com.anatawa12.modPatching.internal

import java.io.File
import java.io.FileNotFoundException

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

fun File.readTextOr(ifNotFound: String = ""): String = try {
    readText()
} catch (e: FileNotFoundException) {
    ifNotFound
}

inline fun <T> List<T>.indexOfFirst(begin: Int, predicate: (T) -> Boolean): Int {
    if (size <= begin) return -1
    var index = begin
    val iter = iterator()
    repeat(begin) {
        if (!iter.hasNext()) return -1
        iter.next()
    }
    for (item in iter) {
        if (predicate(item))
            return index
        index++
    }
    return -1
}

fun <K, V> Map<K, V>.zipEitherByKey(other: Map<K, V>): Map<K, Pair<V?, V?>> {
    val result = mutableMapOf<K, Pair<V?, V?>>()
    for ((k, v) in this) {
        result[k] = v to other[k]
    }
    for (k in (other.keys - keys)) {
        result[k] = null to other[k]
    }
    return result
}
