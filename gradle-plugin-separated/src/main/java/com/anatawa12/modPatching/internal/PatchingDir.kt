package com.anatawa12.modPatching.internal

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File
import java.util.*

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
    @Serializable(SortedMapSerializer::class)
    val mods: SortedMap<String, ModInfo> = TreeMap()
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
) {
    companion object {
        fun create(
            base: ModInfo?,
            patchPath: RelativePathFromProjectRoot,
            sourcePath: RelativePathFromProjectRoot,
            unmodifiedsJar: RelativePathFromProjectRoot,
            sourceJar: RelativePathFromCacheRoot,
            deobfJar: RelativePathFromCacheRoot,
        ) = ModInfo(
            patchPath,
            sourcePath,
            unmodifiedsJar,
            sourceJar,
            deobfJar,
            base?.changedClasses ?: mutableSetOf(),
        )
    }
}

@Serializable
data class LocalConfig(
    @SerialName("cache-base")
    val cache_base: String,
)

class SortedMapSerializer<K, V>(kSerializer: KSerializer<K>, vSerializer: KSerializer<V>) :
    KSerializer<SortedMap<K, V>> {
    private val mapSerializer = MapSerializer(kSerializer, vSerializer)

    override val descriptor: SerialDescriptor get() = mapSerializer.descriptor

    override fun serialize(encoder: Encoder, value: SortedMap<K, V>) {
        mapSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): SortedMap<K, V> {
        return mapSerializer.deserialize(decoder).toMap(TreeMap())
    }
}
