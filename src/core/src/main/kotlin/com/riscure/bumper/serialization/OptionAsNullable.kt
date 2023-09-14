package com.riscure.bumper.serialization

import arrow.core.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Type alias to Arrow's [Option] to indicate it should serialize as a nullable */
typealias OptionAsNullable<T> = @Serializable(ArrowOptionAsNullableSerializer::class) Option<T>

class ArrowOptionAsNullableSerializer<T>(private val dataSerializer: KSerializer<T?>): KSerializer<Option<T>> {
    override val descriptor: SerialDescriptor = dataSerializer.descriptor

    override fun deserialize(decoder: Decoder): Option<T> =
        dataSerializer.deserialize(decoder).toOption()

    override fun serialize(encoder: Encoder, value: Option<T>) =
        dataSerializer.serialize(encoder, value.orNull())
}