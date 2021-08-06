package com.anatawa12.modPatching.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@JvmInline
@Serializable(with = RelativePathFromCacheRoot.Serializer::class)
value class RelativePathFromCacheRoot private constructor(private val path: String) {
    override fun toString(): String = path

    fun join(relative: String): RelativePathFromCacheRoot {
        require(checkPath(path)) { "invalid relative path" }
        return RelativePathFromCacheRoot("$path/$relative")
    }

    object Serializer : KSerializer<RelativePathFromCacheRoot> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("RelativePathFromCacheRoot", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: RelativePathFromCacheRoot) {
            encoder.encodeString(value.path)
        }

        override fun deserialize(decoder: Decoder): RelativePathFromCacheRoot {
            return of(decoder.decodeString())
        }
    }

    companion object {
        fun of(path: String): RelativePathFromCacheRoot {
            require(checkPath(path)) { "invalid path" }
            return RelativePathFromCacheRoot(path)
        }
    }
}

@JvmInline
@Serializable(with = RelativePathFromProjectRoot.Serializer::class)
value class RelativePathFromProjectRoot private constructor(private val path: String) {
    override fun toString(): String = path

    fun join(relative: String): RelativePathFromProjectRoot {
        require(checkPath(path)) { "invalid relative path" }
        return RelativePathFromProjectRoot("$path/$relative")
    }

    object Serializer : KSerializer<RelativePathFromProjectRoot> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("RelativePathFromCacheRoot", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: RelativePathFromProjectRoot) {
            encoder.encodeString(value.path)
        }

        override fun deserialize(decoder: Decoder): RelativePathFromProjectRoot {
            return of(decoder.decodeString())
        }
    }

    companion object {
        fun of(path: String): RelativePathFromProjectRoot {
            require(checkPath(path)) { "invalid path" }
            return RelativePathFromProjectRoot(path)
        }
    }
}

private fun checkPath(path: String): Boolean {
    if (path == "") return false

    for (component in path.split('/')) {
        if (component == "") return false
        if (component == ".") return false
        if (component.any { it in "<>:\"\\/|?:" && it in '\u0000'..'\u001f' })
            return false
    }
    return true
}
