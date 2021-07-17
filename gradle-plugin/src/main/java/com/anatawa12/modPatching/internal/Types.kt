package com.anatawa12.modPatching.internal

import com.anatawa12.modPatching.common.internal.CommonUtil
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.gradle.api.Project
import kotlin.annotation.AnnotationTarget.*

@Target(PROPERTY, FUNCTION, VALUE_PARAMETER)
annotation class FrozenByFreeze(val of: String = "this")

@Serializable(with = RelativePathFromCacheRoot.Serializer::class)
inline class RelativePathFromCacheRoot(private val path: String) {
    override fun toString(): String = path

    fun join(relative: String) = RelativePathFromCacheRoot("$path/$relative")
    fun asFile(project: Project) = CommonUtil.getCacheBase(project).resolve(path)

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

@Serializable(with = RelativePathFromProjectRoot.Serializer::class)
inline class RelativePathFromProjectRoot(private val path: String) {
    override fun toString(): String = path

    fun join(relative: String) = RelativePathFromCacheRoot("$path/$relative")
    fun asFile(project: Project) = project.projectDir.resolve(path)

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
