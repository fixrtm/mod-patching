package com.anatawa12.modPatching.internal.patchingDir

import com.anatawa12.modPatching.OnRepoPatchSource
import com.anatawa12.modPatching.OnVCSPatchSource
import com.anatawa12.modPatching.internal.escapePathStringForFile
import com.anatawa12.modPatching.internal.escapeStringForFile
import com.anatawa12.modPatching.internal.unescapeStringForFile
import java.io.File
import java.io.FileNotFoundException
import kotlin.reflect.KProperty

class PatchingDir(val dir: File) {
    private val _patchingMods = mutableMapOf<String, PatchingMod>()
    internal val changed = mutableSetOf<PatchingMod>()

    init {
        dir.listFiles()?.asSequence().orEmpty()
            .filter { it.isDirectory || it.extension == MOD_DIR_EXTENSION }
            .map { PatchingMod(it, this) }
            .forEach { _patchingMods[it.dir.name.removeSuffix(".$MOD_DIR_EXTENSION")] = it }
    }

    val patchingMods: Collection<PatchingMod> get() = _patchingMods.values

    fun add(name: String): PatchingMod {
        val new = PatchingMod(dir.resolve("$name.$MOD_DIR_EXTENSION"), this)
        _patchingMods[name] = new
        changed += new
        return new
    }

    fun flush() {
        for (mod in changed) {
            mod.flush()
        }
        dir.resolve(".gitkeep").appendText("")
    }

    fun getOrNew(name: String): PatchingMod {
        return _patchingMods[name] ?: add(name)
    }

    companion object {
        fun on(dir: File) = PatchingDir(dir.resolve(PATCHING_DIR_NAME))
    }
}

class PatchingMod(val dir: File, val parent: PatchingDir) {
    private val cache = mutableMapOf<PatchingModProp<*>, Any?>()
    private val changed = mutableSetOf<PatchingModProp<*>>()

    var onRepo by PatchingModProp.OnRepo
    var onVcs by PatchingModProp.OnVcs
    var sourceJarPath by PatchingModProp.SourceJarPath
    var sourcePath by PatchingModProp.SourcePath
    var patchPath by PatchingModProp.PatchPath
    var modifiedClasses by PatchingModProp.ModifiedClasses

    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun <T> PatchingModProp<T>.getValue(self: PatchingMod, property: KProperty<*>): T {
        return this.type.cast(self.cache[this]) ?: kotlin.run {
            this.readFile(dir).also {
                if (defaultValue !== it)
                    self.cache[this] = it
            }
        }
    }
    @Suppress("NOTHING_TO_INLINE")

    private inline operator fun <T> PatchingModProp<T>.setValue(self: PatchingMod, property: KProperty<*>, value: T) {
        if (self.cache[this] == value) return
        self.cache[this] = type.cast(value)
        changed += this
        parent.changed += self
    }

    internal fun flush() {
        dir.resolve(".gitignore").writeText("""
                .gitignore
                ${PatchingModProp.OnRepo.onFile}
                ${PatchingModProp.SourceJarPath.onFile}
                ${PatchingModProp.SourcePath.onFile}
                ${PatchingModProp.PatchPath.onFile}
                
            """.trimIndent())
        for (prop in changed) {
            prop.flush()
        }
    }

    private fun <T> PatchingModProp<T>.flush() {
        dir.mkdirs()
        writeFile(dir, type.cast(cache[this]!!))
    }
}

private sealed class PatchingModProp<T>(val type: Class<T>, val onFile: String) {
    object OnRepo : PatchingModProp<OnRepoPatchSource>(classOf(), "on-repo.txt") {
        override fun parseFile(lines: List<String>) = OnRepoPatchSource.valueOf(lines.first().trim())
        override fun writeFile(value: OnRepoPatchSource) = listOf(value.name)
    }

    object OnVcs : PatchingModProp<OnVCSPatchSource>(classOf(), "on-vcs.txt") {
        override fun parseFile(lines: List<String>) = OnVCSPatchSource.valueOf(lines.first().trim())
        override fun writeFile(value: OnVCSPatchSource) = listOf(value.name)
    }

    object SourceJarPath : PatchingModProp<File>(classOf(), "source-jar-path.txt") {
        override fun parseFile(lines: List<String>) = File(lines.first().unescapeStringForFile())
        override fun writeFile(value: File) = listOf(value.absolutePath.escapePathStringForFile())
    }

    object SourcePath : PatchingModProp<File>(classOf(), "source-path.txt") {
        override fun parseFile(lines: List<String>) = File(lines.first().unescapeStringForFile())
        override fun writeFile(value: File) = listOf(value.absolutePath.escapePathStringForFile())
    }

    object PatchPath : PatchingModProp<File>(classOf(), "patch-path.txt") {
        override fun parseFile(lines: List<String>) = File(lines.first().unescapeStringForFile())
        override fun writeFile(value: File) = listOf(value.absolutePath.escapePathStringForFile())
    }

    object ModifiedClasses : PatchingModProp<Set<String>>(classOf(), "modified-classes.txt") {
        override val defaultValue: Set<String> = emptySet<String>().toSet()
        override fun readFile(dir: File): Set<String> = try {
            super.readFile(dir)
        } catch (e: FileNotFoundException) {
            defaultValue
        }
        override fun parseFile(lines: List<String>) = lines.mapTo(mutableSetOf()) { it.unescapeStringForFile() }
        override fun writeFile(value: Set<String>) = value.map { it.escapeStringForFile() }.sorted()
    }

    open val defaultValue: T? = null
    open fun readFile(dir: File): T = parseFile(dir.resolve(onFile).readLines().filter { it.isNotBlank() })
    fun writeFile(dir: File, value: T) {
        dir.resolve(onFile).writer().buffered().use {
            for (line in writeFile(value)) {
                it.append("$line\n")
            }
        }
    }

    abstract fun parseFile(lines: List<String>): T
    abstract fun writeFile(value: T): List<String>
}

private inline fun <reified T> classOf(): Class<T> = T::class.java

private const val PATCHING_DIR_NAME = ".patching-mods"
private const val MOD_DIR_EXTENSION = "patching-mod"
