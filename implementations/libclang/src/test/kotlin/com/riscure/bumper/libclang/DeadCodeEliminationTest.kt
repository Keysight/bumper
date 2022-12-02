package com.riscure.bumper.libclang

import com.riscure.bumper.transform.eliminateDeadDeclarations
import org.junit.jupiter.api.DisplayName
import kotlin.test.*

class DeadCodeEliminationTest: LibclangTestBase() {

    @DisplayName("Simple mutual unit pair dead struct and dead function def")
    @Test
    fun test00() = bumped(
        """
        struct S {}; // dead
        void g() {}; // dead
        void h();    // alive
        void f() {   // alive by other unit
          h();
        }
        void h() {}  // alive
        """.trimIndent(),

        """
        void f();
        void main() {
          f();
        }
        """.trimIndent()

    ) { units ->
        val (unit1, unit2) = units

        val main       = assertNotNull(unit2.first.functions.find { it.ident == "main" })
        val entrypoint = setOf(main.mkSymbol(unit2.first.tuid))

        val minimal = eliminateDeadDeclarations(units.map { it.second }, entrypoint).assertOK()

        val minUnit1 = assertNotNull(minimal.find { it.tuid == unit1.first.tuid })
        val minUnit2 = assertNotNull(minimal.find { it.tuid == unit2.first.tuid })

        assertEquals(3, minUnit1.declarations.size)
        assertEquals(0, minUnit1.structs.size)
        assertNull(minUnit1.functions.find { it.ident == "g" })

        assertEquals(2, minUnit2.declarations.size)
    }

}