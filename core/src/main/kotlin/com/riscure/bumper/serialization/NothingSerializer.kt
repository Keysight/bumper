package com.riscure.bumper.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object NothingSerializer: KSerializer<Nothing> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("kotlin.Nothing") {}

    override fun deserialize(decoder: Decoder): Nothing =
        throw UnsupportedOperationException("Nothing does not have instances")

    override fun serialize(encoder: Encoder, value: Nothing): Nothing =
        throw UnsupportedOperationException("Nothing cannot be serialized")
}
