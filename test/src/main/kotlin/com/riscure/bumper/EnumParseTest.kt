package com.riscure.bumper

import com.riscure.bumper.ast.*
import com.riscure.bumper.ast.Storage
import com.riscure.bumper.parser.UnitState
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.*

interface EnumParseTest<E,S,U: UnitState<E, S>> : ParseTestBase<E, S, U> {

    @Test
    @DisplayName("Named enum")
    fun test00() = parsedAndRoundtrip("""
        enum E { X, Y, Z };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val enum = assertIs<UnitDeclaration.Enum>(ast.declarations[0])

        assertEquals("E", enum.ident)
        assertEquals(Storage.Default, enum.storage)
        assertEquals(EntityKind.Enum, enum.kind)
        assertTrue(enum.isDefinition)

        val enums = enum.enumerators.assertOK()
        assertEquals(3, enums.size)
        assertEquals(Enumerator("X", 0L, enum.tlid), enums[0])
        assertEquals(Enumerator("Y", 1L, enum.tlid), enums[1])
        assertEquals(Enumerator("Z", 2L, enum.tlid), enums[2])
    }

    @Test
    @DisplayName("Named enum with fixed enumerator value")
    fun test01() = parsedAndRoundtrip("""
        enum E { X, Y = 99, Z };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val enum = assertIs<UnitDeclaration.Enum>(ast.declarations[0])

        assertEquals("E", enum.ident)
        assertEquals(Storage.Default, enum.storage)
        assertEquals(EntityKind.Enum, enum.kind)
        assertTrue(enum.isDefinition)

        val enums = enum.enumerators.assertOK()
        assertEquals(3, enums.size)
        assertEquals(Enumerator("X", 0L, enum.tlid), enums[0])
        assertEquals(Enumerator("Y", 99L, enum.tlid), enums[1])
        assertEquals(Enumerator("Z", 100L, enum.tlid), enums[2])
    }


    @Test
    @DisplayName("Named enum with expression enumerator value")
    fun test02() = parsedAndRoundtrip("""
        enum E { X, Y = 20 + 22, Z };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val enum = assertIs<UnitDeclaration.Enum>(ast.declarations[0])

        assertEquals("E", enum.ident)
        assertEquals(Storage.Default, enum.storage)
        assertEquals(EntityKind.Enum, enum.kind)
        assertTrue(enum.isDefinition)

        val enums = enum.enumerators.assertOK()
        assertEquals(3, enums.size)
        assertEquals(Enumerator("X", 0L, enum.tlid), enums[0])
        assertEquals(Enumerator("Y", 42L, enum.tlid), enums[1])
        assertEquals(Enumerator("Z", 43L, enum.tlid), enums[2])
    }

    @Test
    @DisplayName("Whitespace and expressions")
    fun test03() = parsedAndRoundtrip("""
        enum E { X
            =

                    1
                    ,
          Y = 20


            +
                22

                ,
            Z

                =
                    4
             *


                4
        };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val enum = assertIs<UnitDeclaration.Enum>(ast.declarations[0])

        assertEquals("E", enum.ident)
        assertEquals(Storage.Default, enum.storage)
        assertEquals(EntityKind.Enum, enum.kind)
        assertTrue(enum.isDefinition)

        val enums = enum.enumerators.assertOK()
        assertEquals(3, enums.size)
        assertEquals(Enumerator("X", 1L, enum.tlid), enums[0])
        assertEquals(Enumerator("Y", 42L, enum.tlid), enums[1])
        assertEquals(Enumerator("Z", 16L, enum.tlid), enums[2])
    }

    @Test
    @DisplayName("Typedeffed enum")
    fun test04() = parsedAndRoundtrip("""
        typedef enum { eMediumDPI, eHighDPI, eRetina } DPI;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val enumTypedef = assertIs<UnitDeclaration.Typedef>(ast.typedefs.find { it.ident == "DPI" })
        val enum = assertIs<UnitDeclaration.Enum>(ast.enums.find {
            it.tlid == assertIs<Type.Enum>(enumTypedef.underlyingType).ref
        })
        assertEquals(Storage.Default, enum.storage)
        assertEquals(EntityKind.Enum, enum.kind)
        assertTrue(enum.isDefinition)

        val enums = enum.enumerators.assertOK()
        assertEquals(3, enums.size)
        assertEquals(Enumerator("eMediumDPI", 0L, enum.tlid), enums[0])
        assertEquals(Enumerator("eHighDPI", 1L, enum.tlid), enums[1])
        assertEquals(Enumerator("eRetina", 2L, enum.tlid), enums[2])
    }
}