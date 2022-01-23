package com.anatawa12.modPatching.binary

import com.anatawa12.modPatching.binary.internal.isSameDataStream
import com.anatawa12.modPatching.source.internal.readTextOr
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

open class ListModifiedClasses : DefaultTask() {
    @InputFiles
    val oldJars = project.objects.fileCollection()

    @InputFile
    val newerJar = project.objects.fileProperty()

    @OutputDirectory
    val modifiedInfoDir = project.objects.directoryProperty()

    @TaskAction
    fun generate() {
        val modifiedInfoDir = modifiedInfoDir.get()
        modifiedInfoDir.asFile.deleteRecursively()

        ZipFiles(oldJars.files).use { zips ->
            project.zipTree(newerJar).visit {
                if (this.isDirectory) return@visit
                if (!this.path.endsWith(".class")) return@visit
                MultiClose(zips.find(this.path)).use { streams ->
                    if (streams.isNotEmpty()) {
                        this.open().use { entryStream ->
                            if (isSameDataStream(entryStream, streams)) {
                                write(modifiedInfoDir.file(this.path), "SAME")
                            } else {
                                write(modifiedInfoDir.file(this.path), "MODIFIED")
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun isModified(path: String): Boolean {
        return modifiedInfoDir.get().file(path).asFile.readTextOr("UNMODIFIED")
            .let(ModifiedType::valueOf) == ModifiedType.MODIFIED
    }

    internal fun isUnmodified(path: String): Boolean {
        return modifiedInfoDir.get().file(path).asFile.readTextOr("UNMODIFIED")
            .let(ModifiedType::valueOf) == ModifiedType.UNMODIFIED
    }

    private enum class ModifiedType {
        SAME,
        MODIFIED,
        UNMODIFIED,
    }

    private fun write(file: RegularFile, body: String) {
        file.asFile.apply { parentFile.mkdirs() }.writeText(body)
    }

    private class MultiClose<C : Closeable>(private val internals: List<C>) : Closeable, List<C> by internals {
        override fun close() {
            var exception: Throwable? = null
            for (file in internals.asReversed()) {
                try {
                    file.close()
                } catch (e: Throwable) {
                    if (exception != null) exception.addSuppressed(e)
                    else exception = e
                }
            }
            if (exception != null) throw exception
        }
    }

    private class ZipFiles(private val files: List<ZipFile>) : Closeable by MultiClose(files) {
        constructor(files: Collection<File>) : this(files.mapNotNullWithFinally(::ZipFile))

        fun find(path: String): List<InputStream> {
            return files.mapNotNullWithFinally { it.getEntry(path)?.let(it::getInputStream) }
        }

        companion object {
            @JvmStatic
            fun <T, R : Closeable> Iterable<T>.mapNotNullWithFinally(f: (T) -> R?): List<R> {
                val list = mutableListOf<R>()
                try {
                    for (v in this) f(v)?.let(list::add)
                } catch (t: Throwable) {
                    for (zip in list) try {
                        zip.close()
                    } catch (e: Throwable) {
                        t.addSuppressed(e)
                    }
                    throw t
                }
                return list
            }
        }
    }
}
