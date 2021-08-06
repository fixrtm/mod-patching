package com.anatawa12.modPatching.internal

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.Remapper
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DeObfuscator {
    fun deobfucate(
        sourceJar: File,
        destination: File,
        mappings: File,
    ) {
        destination.parentFile.mkdirs()

        val methodMap: Map<String, String>
        val fieldMap: Map<String, String>
        ZipFile(mappings).use { mappingJar ->
            methodMap = readCsv(mappingJar, "methods.csv")
            fieldMap = readCsv(mappingJar, "fields.csv")
        }
        val remapper = MapRemapper(methodMap, fieldMap)

        sourceJar.inputStream().buffered().let { ZipInputStream(it) }.use { zis ->
            destination.outputStream().buffered().let { ZipOutputStream(it) }.use { zos ->
                for (entry in ZipEntryIterator(zis)) {
                    if (entry.name.endsWith(".class")) {
                        // it's class file: remap
                        val cr = ClassReader(zis.readBytes())
                        val cw = ClassWriter(0)
                        cr.accept(LambdaSupportedClassRemapper(cw, remapper), 0)
                        zos.putNextEntry(ZipEntry(entry.name).apply {
                            size = 0
                            crc = 0
                            compressedSize = -1
                            time = entry.time
                            entry.lastAccessTime?.let { lastAccessTime = null }
                            entry.creationTime?.let { creationTime = null }
                            extra = entry.extra
                            comment = entry.comment
                        })
                        zos.write(cw.toByteArray())
                        zos.closeEntry()
                        zis.closeEntry()
                    } else {
                        // it's not class file: just copy
                        zos.putNextEntry(ZipEntry(entry).apply { compressedSize = -1 })
                        zis.copyTo(zos)
                        zos.closeEntry()
                        zis.closeEntry()
                    }
                }
            }
        }
    }

    private fun readCsv(zip: ZipFile, name: String): Map<String, String> =
        zip.getInputStream(zip.getEntry(name) ?: error("no $name found in mapping jar"))
            .bufferedReader()
            .useLines { lines ->
                lines
                    .drop(1)
                    .map { line -> line.split(',', limit = 3).let { it[0].trim() to it[1].trim() } }
                    .toMap()
            }

    private class MapRemapper(
        val methodMap: Map<String, String>,
        val fieldMap: Map<String, String>,
    ) : Remapper() {
        override fun mapMethodName(owner: String?, name: String, desc: String?): String = methodMap[name] ?: name
        override fun mapFieldName(owner: String?, name: String, desc: String?): String = fieldMap[name] ?: name
    }

    private class ZipEntryIterator(val zis: ZipInputStream) : Iterator<ZipEntry> {
        var entry: ZipEntry? = null

        override fun hasNext(): Boolean {
            entry = entry ?: zis.nextEntry
            return entry != null
        }

        override fun next(): ZipEntry {
            entry = entry ?: zis.nextEntry
            val result = entry ?: throw NoSuchElementException()
            entry = null
            return result
        }
    }
}
