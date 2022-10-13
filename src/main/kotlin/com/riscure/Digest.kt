package com.riscure

import arrow.typeclasses.Monoid
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.*

data class Digest(val bytes: ByteArray) {

    fun plus(b: Digest): Digest {
        val h = Digest.hasher()
        h.update(this.bytes)
        h.update(b.bytes)
        return Digest(h.digest())
    }

    companion object {
        fun hasher() = MessageDigest.getInstance("SHA-1")
        fun empty(): Digest = Digest(hasher().digest())

        fun of(vararg inputs: ByteArray) = Digest(
            hasher()
                .apply { inputs.forEach { update(it) }}
                .digest())

        fun combineAll(digests: List<Digest>) = Digest(
            hasher()
                .apply {digests.forEach { update(it.bytes) }}
                .digest())
    }
}

fun File.digest(): Digest {
    return DigestInputStream(this.inputStream(), Digest.hasher())
        .use {
            it.readAllBytes()
            Digest(it.messageDigest.digest())
        }
}

fun String.digest(): Digest = Digest.of(this.toByteArray())
fun List<String>.digest(): Digest = Digest.combineAll(this.map { it.digest() })