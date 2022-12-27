package com.riscure.bumper

import arrow.core.None
import arrow.core.getOrElse
import arrow.core.some
import com.riscure.bumper.ast.*
import com.riscure.bumper.ast.Storage
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.index.TUID
import com.riscure.bumper.parser.UnitState
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.*

interface GlobalParseTest<E,S,U: UnitState<E, S>>: ParseTestBase<E, S, U> {

    @Test
    @DisplayName("Global array element assignment")
    fun test00() = parsedAndRoundtrip("""
        static const int xs[1] = {
          42,
        };
    """.trimIndent()) { ast ->
        val xs = ast.variables[0]
        val arType = assertIs<Type.Array>(xs.type)
        assertEquals(1L.some(), arType.size)
    }
}
