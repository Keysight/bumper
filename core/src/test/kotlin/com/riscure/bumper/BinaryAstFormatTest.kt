package com.riscure.bumper

import arrow.core.*
import com.riscure.bumper.ast.*
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Nested
import kotlin.test.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

class BinaryAstFormatTest {

    @Nested
    inner class Types {
        fun roundtrip(type: Type) {
            val bytes = ProtoBuf.encodeToByteArray(type)
            val type2 = ProtoBuf.decodeFromByteArray<Type>(bytes)
            println("ascii: ${bytes.toAsciiHexString()}")
            assertEquals(type, type2)
        }

        @Test fun void() = roundtrip(Type.Void())
        @Test fun int() = roundtrip(Type.int)
        @Test fun char() = roundtrip(Type.char)
        @Test fun function() = roundtrip(
            Type.function(Type.Void(), Param(Site.local, "x".some(), Type.int))
        )
    }

    @Nested
    inner class Declarations {
        fun roundtrip(decl: ErasedDeclaration) {
            val bytes = ProtoBuf.encodeToByteArray(decl)
            val type2 = ProtoBuf.decodeFromByteArray<ErasedDeclaration>(bytes)

            assertEquals(decl, type2)
        }

        @Test fun function() =
            (Site.root + Site.Toplevel(0)).let { site ->
                roundtrip(
                    Declaration.Fun(
                        site,
                        "f",
                        false,
                        Type.Void(),
                        listOf(Param(site + Site.FunctionParam(0), "x".some(), Type.int))
                    )
                )
            }
    }
}