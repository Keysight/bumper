package com.riscure.langs.c.parser.clang

import arrow.core.*
import com.riscure.langs.c.ast.*
import kotlin.test.*
import java.io.File
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
     * This is the preprocessed version of 010-demo-functions.c from the demo workspace.
     * It was produced with `clang -O0 -E 010-demo-functions.c -o 011-preprocessed-demo.c`
     * (Yes, the O0 makes a difference! On clang 16 is does not seem to make a difference.)
     *
     * It #includes stdio.h, so it is a big file. This shows that we can find
     * the declarations from the original file, by filtering by presumedLocation.
     */
    @Test
    fun test011() {
        parsed("/parser-tests/011-preprocessed-demo.c") { tu1, unit1 ->
            // Find function by filtering by original main file name
            val test = tu1.decls.filter { tld ->
                with (unit1) {
                    tld.meta.presumedLocation
                        .map { loc -> loc.sourceFile == Path.of("010-demo-functions.c") }
                        .getOrElse { true }
                }
            }
            // Should only have 3 top-level elements, despite this including stdio.
            assertEquals(3, test.size)

            // We also check that the manually preprocessed source is the same as the
            // result of parsing the unpreprocessed source.
            // the source of func_assign_for_equals in the manually preprocessed file
            // and the parsed unpreprocessed file should be the same
            parsed("/parser-tests/010-demo-functions.c") { tu2, unit2 ->
                val fs1 = tu1.decls
                    .functions()
                    .definitions()
                    .filter { it.name == "func_assign_for_equals" }
                assertEquals(1, fs1.size)
                val fn1 = fs1[0]

                val fs2 = tu2.decls
                    .functions()
                    .definitions()
                    .filter { it.name == "func_assign_for_equals" }
                assertEquals(1, fs2.size)
                val fn2 = fs2[0]

                assertEquals(unit1.getSource(fn1), unit2.getSource(fn2))
            }
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

    @Test
    fun test013() {
        parsed("/parser-tests/013-cpp-define.c") { tu, unit ->
            val ds = tu.decls
            assertEquals(1, ds.size)
            val fn = ds[0]

            val source = unit.getSource(fn).getOrElse { "" }
            assertFalse("DELEIPEMACRO".toRegex().containsMatchIn(source))
        }
    }

    @Test
    fun test014() {
        parsed("/parser-tests/014-cpp-ifdef.c") { tu, unit ->
            val ds = tu.decls
            assertEquals(1, ds.size)
            val fn = ds[0]

            val source = unit.getSource(fn).getOrElse { "" }
            assertFalse("bad".toRegex().containsMatchIn(source))
            println(fn.meta.location)
            println(fn.meta.presumedLocation)
        }
    }

    @Test
    fun test016() {
        parsed("/parser-tests/016-global-with-initializer.c") { tu, unit ->
            val ds = tu.decls
            assertEquals(1, ds.size)
            val x = ds[0]

            println(x)
        }
    }

    @Test
    fun test017() {
        parsed("/parser-tests/017-global-with-array-initializer.c") { tu, unit ->
            val ds = tu.decls
            assertEquals(1, ds.size)
            val x = ds[0]

            println(x)
        }
    }

    @Test
    fun test018() {
        parsed("/parser-tests/018-global-with-function-pointers.c") { tu, unit ->
            val ds = tu.decls

            println(ds)
        }
    }
}