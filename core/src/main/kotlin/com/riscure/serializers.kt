package com.riscure

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class AnyAsUnitSerializer : KSerializer<Any> {
    override fun serialize(encoder: Encoder, value: Any) {
        encoder.encodeBoolean(false)
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("AnyAsUnit", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Any = Unit
}