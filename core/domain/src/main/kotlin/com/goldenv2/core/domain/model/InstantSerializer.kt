package com.goldenv2.core.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

/**
 * Serializes [java.time.Instant] as epoch milliseconds so @Contextual Instant
 * fields can round-trip through JSON (routing config import/export).
 */
object InstantSerializer : KSerializer<Instant> {
    private val delegate = Long.serializer()
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Instant =
        Instant.ofEpochMilli(delegate.deserialize(decoder))

    override fun serialize(encoder: Encoder, value: Instant) {
        delegate.serialize(encoder, value.toEpochMilli())
    }
}
