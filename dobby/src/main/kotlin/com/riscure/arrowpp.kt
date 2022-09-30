@file:OptIn(ExperimentalSerializationApi::class)

package com.riscure

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.some
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class OptionSerializer<T>(private val inner: KSerializer<T>) : KSerializer<Option<T>> {
    override val descriptor: SerialDescriptor = inner.descriptor

    override fun deserialize(decoder: Decoder): Option<T> = with(decoder) {
        if (decodeNotNullMark())
            inner.deserialize(this).some()
        else {
            decodeNull()
            None
        }
    }

    override fun serialize(encoder: Encoder, value: Option<T>) = when(value) {
        None -> encoder.encodeNull()
        is Some -> inner.serialize(encoder, value.value)
    }
}