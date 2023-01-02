package com.riscure

import com.riscure.dobby.clang.Command
import com.riscure.dobby.clang.CompilationDb
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.*

data class Digest(val bytes: ByteArray) {

    fun plus(b: Digest): Digest {
        val h = hasher()
        h.update(this.bytes)
        h.update(b.bytes)
        return Digest(h.digest())
    }

    override fun toString() = "sha1:${Base64.getUrlEncoder().encodeToString(bytes)}"

    /**
     * Get a short string identifier based on the Base64 encoded bytes in the digest.
     */
    fun makeKey(length: Int = 15) = Base64.getUrlEncoder().encodeToString(bytes).take(length)

    companion object {
        @JvmStatic fun hasher(): MessageDigest = MessageDigest.getInstance("SHA-1")

        @JvmStatic fun empty(): Digest = Digest(hasher().digest())

        @JvmStatic fun of(vararg inputs: ByteArray) = Digest(
            hasher()
                .apply { inputs.forEach { update(it) }}
                .digest())

        // overloads for use from Java
        @JvmStatic fun of(str: String) = str.digest()
        @JvmStatic fun of(int: Int) = int.digest()
        @JvmStatic fun of(file: File) = file.digest()
        @JvmStatic fun of(strs: List<String>) = strs.digest()
        @JvmStatic fun of(cmd: Command) = cmd.digest()
        @JvmStatic fun of(entry: CompilationDb.Entry) = entry.digest()

        @JvmStatic fun combineAll(digests: List<Digest>) = Digest(
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

fun Int.digest(): Digest = Digest.of(this.toString())
fun String.digest(): Digest = Digest.of(this.toByteArray())
fun List<String>.digest(): Digest = Digest.combineAll(this.map { it.digest() })
fun Command.digest(): Digest {
    val hashes = optArgs.map { o -> listOf(o.opt.key).plus(o.values).digest() }
        .plus(positionalArgs.digest())

    return Digest.combineAll(hashes)
}
fun CompilationDb.Entry.digest(): Digest =
    Digest.combineAll(listOf(command.digest(), resolvedMainSource.toString().digest()))
