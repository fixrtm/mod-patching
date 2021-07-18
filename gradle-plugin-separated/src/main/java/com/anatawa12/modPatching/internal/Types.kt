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
value class RelativePathFromCacheRoot(private val path: String) {
    override fun toString(): String = path

    fun join(relative: String) = RelativePathFromCacheRoot("$path/$relative")

    object Serializer : KSerializer<RelativePathFromCacheRoot> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("RelativePathFromCacheRoot", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: RelativePathFromCacheRoot) {
            encoder.encodeString(value.path)
        }

        override fun deserialize(decoder: Decoder): RelativePathFromCacheRoot {
            return RelativePathFromCacheRoot(decoder.decodeString())
        }
    }
}

@JvmInline
@Serializable(with = RelativePathFromProjectRoot.Serializer::class)
value class RelativePathFromProjectRoot(private val path: String) {
    override fun toString(): String = path

    fun join(relative: String) = RelativePathFromProjectRoot("$path/$relative")

    object Serializer : KSerializer<RelativePathFromProjectRoot> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("RelativePathFromCacheRoot", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: RelativePathFromProjectRoot) {
            encoder.encodeString(value.path)
        }

        override fun deserialize(decoder: Decoder): RelativePathFromProjectRoot {
            return RelativePathFromProjectRoot(decoder.decodeString())
        }
    }
}
