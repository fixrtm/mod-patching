package com.anatawa12.modPatching.internal

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

private const val PATCHING_DIR_NAME = ".patching-mods"

class PatchingDir private constructor(val root: File) {
    lateinit var local: LocalConfig
    lateinit var main: PatchingMainConfig

    fun load() {
        local = root.resolve("local.yaml")
            .readText()
            .let { Yaml.default.decodeFromString(LocalConfig.serializer(), it) }
        main = root.resolve("main.yaml")
            .readText()
            .let { Yaml.default.decodeFromString(PatchingMainConfig.serializer(), it) }
    }

    fun save() {
        try {
            root.resolve("local.yaml").writeText(Yaml.default.encodeToString(LocalConfig.serializer(), local))
            root.resolve("main.yaml").writeText(Yaml.default.encodeToString(PatchingMainConfig.serializer(), main))
            root.resolve(".gitignore").writeText("local.yaml\n.gitignore\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun on(dir: File) = PatchingDir(dir.resolve(PATCHING_DIR_NAME)).apply { load() }
    }
}

@Serializable
class PatchingMainConfig {
    val mods = mutableMapOf<String, ModInfo>()
}

@Serializable
data class ModInfo(
    // relative from pathing mod root
    @SerialName("patch-path")
    val patchPath: RelativePathFromProjectRoot,
    @SerialName("source-path")
    val sourcePath: RelativePathFromProjectRoot,
    @SerialName("unmodifieds-jar")
    val unmodifiedsJar: RelativePathFromProjectRoot,

    // relative from pathing cache root
    @SerialName("source-jar")
    val sourceJar: RelativePathFromCacheRoot,
    @SerialName("deobf-jar")
    val deobfJar: RelativePathFromCacheRoot,

    // list of class names
    @SerialName("changed-classes")
    val changedClasses: MutableSet<String> = mutableSetOf(),
)

@Serializable
data class LocalConfig(
    @SerialName("cache-base")
    val cache_base: String,
)
