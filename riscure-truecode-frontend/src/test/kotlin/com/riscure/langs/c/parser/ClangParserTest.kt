package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.Failable
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.parser.clang.*
import kotlin.test.*
import java.io.File
import java.io.StringWriter
import java.nio.file.Path

class ClangParserTest {

    private fun parsed(resource: String, whenOk: (ast: TranslationUnit) -> Unit) =
        parsed(resource) { ast, _ -> whenOk(ast) }

    private fun parsed(resource: String, whenOk: (ast: TranslationUnit, state: ClangUnitState) -> Unit) {
        val test = File(ClangParserTest::class.java.getResource(resource)!!.file)
        ClangParser().parse(test).tap { it.use { unit ->
            when (val ast = unit.ast()) {
                is Either.Left -> fail("Expected successful parse, got error: ${ast.value}")
                is Either.Right -> whenOk(ast.value, unit)
            }
        }}
    }


    @Test
    fun test001() {
        parsed("/parser-tests/001-minimal-main.c") { tu ->
            assertEquals(1, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.Fun)

            val fn = tu.decls[0] as TopLevel.Fun
            assertEquals("main", fn.name)
        }
    }

    @Test
    fun test002() {
        parsed("/parser-tests/002-main-with-params.c") { tu ->
            assertEquals(1, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.Fun)

            val fn = tu.decls[0] as TopLevel.Fun
            assertEquals("main", fn.name)

            assertEquals(2, fn.params.size)
            assertEquals("argc", fn.params[0].name)
            assertEquals(Type.Int(IKind.IInt), fn.params[0].type)
            assertEquals("argv", fn.params[1].name)
            assertEquals(Type.Array(Type.Ptr(Type.Int(IKind.IChar))), fn.params[1].type)
        }
    }

    @Test
    fun test003() {
        parsed("/parser-tests/003-function-decl.c") { tu ->
            assertEquals(1, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.Fun)
        }
    }

    @Test
    fun test004() {
        parsed("/parser-tests/004-struct-decl.c") { tu ->
            assertEquals(1, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.Composite)
            assertEquals(StructOrUnion.Struct, (tu.decls[0] as TopLevel.Composite).structOrUnion)
        }
    }

    @Test
    fun test005() {
        parsed("/parser-tests/005-struct-def.c") { tu ->
            assertEquals(1, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.Composite)
            assertEquals(StructOrUnion.Struct, (tu.decls[0] as TopLevel.Composite).structOrUnion)
        }
    }

    @Test
    fun test006() {
        parsed("/parser-tests/006-anonymous-struct.c") { tu ->
            assertEquals(2, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.Composite)
            assertEquals(StructOrUnion.Struct, (tu.decls[0] as TopLevel.Composite).structOrUnion)
        }
    }

    @Test
    fun test007() {
        parsed("/parser-tests/007-named-struct-with-var.c") { tu ->
            assertEquals(3, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.Composite)

            assertTrue(tu.decls[1] is TopLevel.Var)
            val book = tu.decls[1] as TopLevel.Var
            assertEquals(Type.Struct("Book"), book.type)

            assertTrue(tu.decls[2] is TopLevel.Var)
            val another = tu.decls[2] as TopLevel.Var
            assertEquals(Type.Struct("Book"), another.type)

        }
    }

    @Test
    fun test008() {
        parsed("/parser-tests/008-typedef.c") { tu ->
            assertEquals(3, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.Composite)
            assertTrue(tu.decls[1] is TopLevel.Typedef)

            assertTrue(tu.decls[2] is TopLevel.Var)
            val another = tu.decls[2] as TopLevel.Var
            assertEquals(Type.Named("mybook", Type.Struct("Book")), another.type)
        }
    }

    @Test
    fun test009() {
        parsed("/parser-tests/009-fun-with-doc.c") { tu ->
            // FIXME
            println(tu)
            fail()
        }
    }

    @Test
    fun test010() {
        parsed("/parser-tests/010-demo-functions.c") { tu, unit ->
            val fs = tu.decls
                .functions()
                .definitions()
                .filter { it.name == "func_assign_for_equals" }

            val f = fs[0]
        }
    }

    /**
     * This is the preprocessed version of AssignForEquals.c from the demo workspace.
     * It #includes stdio.h, so it is a big file. This shows that we can find
     * the declarations from the original file, by filtering by presumedLocation.
     */
    @Test
    fun test011() {
        parsed("/parser-tests/011-preprocessed-demo.c") { tu, unit ->
            val test = tu.decls.filter { tld ->
                with (unit) {
                    tld.meta.presumedLocation
                        .map { loc -> loc.sourceFile == Path.of("AssignForEquals.c") }
                        .getOrElse { true }
                }
            }

            assertEquals(1, test.size)
            assertEquals("func_assign_for_equals", test[0].name)
        }
    }

    @Test
    fun test012() {
        parsed("/parser-tests/012-line-directive.c") { tu ->
            val fn = tu.decls[0]
            when (val loc = fn.meta.presumedLocation) {
                is None -> fail()
                is Some -> {
                    assertEquals(Path.of("haha.c"), loc.value.sourceFile)
                    assertEquals(42, loc.value.row)
                }
            }

            val gn = tu.decls[1]
            when (val loc = gn.meta.presumedLocation) {
                is None -> fail()
                is Some -> {
                    assertEquals(Path.of("werktook.c"), loc.value.sourceFile)
                    assertEquals(19, loc.value.row)
                }
            }
        }
    }

    /* writing source test */
    @Test
    fun test013() {
        parsed("/parser-tests/001-minimal-main.c") { tu, unit ->
            val ds = tu.decls
            val writer = object: StringWriter(), Failable by Failable.tantrum {}

            unit.writeSource(ds, writer)
            assertEquals("\nint main() {\n}\n", writer.toString())
        }
    }
}