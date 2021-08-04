package com.anatawa12.modPatching.binary.internal

import org.gradle.api.Project
import org.gradle.api.file.FileTree
import java.io.File
import java.io.InputStream

object Util {
    fun getBuildPath(project: Project, vararg pathElements: String): File {
        var cache = project.buildDir
        cache = cache.resolve("patching-mod")
        for (pathElement in pathElements) {
            cache = cache.resolve(pathElement)
        }
        return cache
    }
}

fun Iterable<FileTree>.flatten(project: Project): FileTree {
    var tree = project.files().asFileTree
    forEach { tree += it }
    return tree
}

fun InputStream.readFully(buf: ByteArray): Int {
    var cursor = 0
    while (cursor != buf.size) {
        val read = read(buf, cursor, buf.size - cursor)
        if (read == -1) return -1
        cursor += read
    }
    return cursor// == buf.size
}

fun isSameDataStream(stream0: InputStream, stream1: InputStream): Boolean {
    val buf0 = ByteArray(1024)
    val buf1 = ByteArray(1024)
    var size0: Int
    var size1: Int
    while (true) {
        size0 = stream0.readFully(buf0)
        size1 = stream1.readFully(buf1)
        // size changed (in stream): not same
        if (size0 != size1) return false
        // all elements are read and same: same stream
        if (size0 == -1) return true
        for (i in 0 until size0) {
            if (buf0[i] != buf1[i]) return false
        }
    }
}
