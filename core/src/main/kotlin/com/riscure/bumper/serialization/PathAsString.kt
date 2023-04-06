package com.riscure.bumper.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path

/**
 * Alias for java.nio.Path to indicate that it should jus be serialized as a plain string
 */
typealias PathAsString = @Serializable(PathAsStringSerializer::class) Path

class PathAsStringSerializer: KSerializer<Path> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): Path =
        String.serializer().deserialize(decoder).let { Path.of(it) }

    override fun serialize(encoder: Encoder, value: Path) =
        String.serializer().serialize(encoder, value.toString())
}
