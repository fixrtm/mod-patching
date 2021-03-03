package com.anatawa12.modPatching

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

open class DeobfuscateSrg : DefaultTask() {
    @InputFile
    val sourceJar = project.objects.property(File::class)
    @OutputFile
    val destination = project.objects.property(File::class)
    @InputFile
    val methodCsv = project.objects.property(File::class)
    @InputFile
    val fieldCsv = project.objects.property(File::class)

    @TaskAction
    fun deobfucate() {
        val sourceJar = sourceJar.get()
        val destination = destination.get()
        val methodCsv = methodCsv.get()
        val fieldCsv = fieldCsv.get()
        destination.parentFile.mkdirs()

        val methodMap = readCsv(methodCsv)
        val fieldMap = readCsv(fieldCsv)
        val remapper = MapRemapper(methodMap, fieldMap)

        sourceJar.inputStream().buffered().let { ZipInputStream(it) }.use { zis ->
            destination.outputStream().buffered().let { ZipOutputStream(it) }.use { zos ->
                for (entry in ZipEntryIterator(zis)) {
                    if (entry.name.endsWith(".class")) {
                        // it's class file: remap
                        val cr = ClassReader(zis.readBytes())
                        val cw = ClassWriter(0)
                        cr.accept(ClassRemapper(cw, remapper), 0)
                        zos.putNextEntry(entry)
                        zos.write(cw.toByteArray())
                        zos.closeEntry()
                        zis.closeEntry()
                    } else {
                        // it's not class file: just copy
                        zos.putNextEntry(entry)
                        zis.copyTo(zos)
                        zos.closeEntry()
                        zis.closeEntry()
                    }
                }
            }
        }
    }

    private fun readCsv(csv: File) = csv.useLines { lines ->
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
