package com.riscure.bumper

import com.riscure.bumper.ast.Type
import com.riscure.bumper.index.Index
import com.riscure.bumper.parser.UnitState
import com.riscure.dobby.clang.Options
import org.junit.jupiter.api.*
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

interface IndexTest<E,S,U: UnitState<E, S, U>>: ParseTestBase<E,S,U> {

    fun indexRoundtrip(program: String, opts: Options = listOf(), test: (Index) -> Unit = {}) =
        bumped(program, opts) { ast, _ ->
            val out = ByteArrayOutputStream()
            Index.of(ast)
                .apply {
                    // serialize to a byte array in memory
                    write(out)
                    val serialized = out.toByteArray()
                    // deserialize again
                    val that = serialized
                        .inputStream()
                        .use { Index.read(it) }
                        .assertOK()

                    // make sure it is the same
                    assertEquals(this, that)

                    // run the test's assertions on the initial index
                    test(this)
                }
        }

    @Test
    fun `one-zero-arg-function-definition`() = indexRoundtrip("""
        void f() {}
    """.trimIndent()) { index ->
        val fs = index.symbols["f"].assertOK()
        assertEquals(1, fs.size)
        val f = fs.first()
        assertEquals(Type.function(Type.void), f.proto.type)
    }
}