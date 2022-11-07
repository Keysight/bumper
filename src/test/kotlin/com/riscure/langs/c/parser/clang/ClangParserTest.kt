package com.riscure.langs.c.parser.clang

import arrow.core.*
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.pp.Pretty
import org.junit.jupiter.api.Disabled
import kotlin.test.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.writeText

class ClangParserTest {

    private fun literal(cstring: String, whenOk: (ast: ErasedTranslationUnit) -> Unit) {
        val file: File = kotlin.io.path.createTempFile(suffix=".c").apply { writeText(cstring) } .toFile()
        parsed(file, whenOk)
    }

    private fun parsed(resource: String, whenOk: (ast: ErasedTranslationUnit) -> Unit) {
        val test = File(javaClass.getResource(resource)!!.file)
        parsed(test, whenOk)
    }

    private fun parsed(test: File, whenOk: (ast: ErasedTranslationUnit) -> Unit) {
        ClangParser().parse(test).tap { it.use { unit ->
            when (val ast = unit.ast) {
                is Either.Left -> fail("Expected successful parse, got error", ast.value)
                is Either.Right -> whenOk(ast.value)
            }
        }}
    }


    @Test
    fun test001() {
        parsed("/parser-tests/001-minimal-main.c") { tu ->
            assertEquals(1, tu.toplevelDeclarations.size)
            assertTrue(tu.toplevelDeclarations[0] is Declaration.Fun)

            val fn = tu.toplevelDeclarations[0] as Declaration.Fun
            assertEquals("main", fn.name)
        }
    }

    @Test
    fun test002() {
        parsed("/parser-tests/002-main-with-params.c") { tu ->
            assertEquals(1, tu.toplevelDeclarations.size)
            assertTrue(tu.toplevelDeclarations[0] is Declaration.Fun)

            val fn = tu.toplevelDeclarations[0] as Declaration.Fun
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
            assertEquals(1, tu.toplevelDeclarations.size)
            assertTrue(tu.toplevelDeclarations[0] is Declaration.Fun)
        }
    }

    @Test
    fun test004() {
        parsed("/parser-tests/004-struct-decl.c") { tu ->
            assertEquals(1, tu.toplevelDeclarations.size)
            assertTrue(tu.toplevelDeclarations[0] is Declaration.Composite)
            assertEquals(StructOrUnion.Struct, (tu.toplevelDeclarations[0] as Declaration.Composite).structOrUnion)
        }
    }

    @Test
    fun test005() {
        parsed("/parser-tests/005-struct-def.c") { tu ->
            assertEquals(1, tu.toplevelDeclarations.size)
            assertTrue(tu.toplevelDeclarations[0] is Declaration.Composite)
            assertEquals(StructOrUnion.Struct, (tu.toplevelDeclarations[0] as Declaration.Composite).structOrUnion)
        }
    }

    @Test
    fun test006() {
        parsed("/parser-tests/006-anonymous-struct.c") { tu ->
            assertEquals(2, tu.toplevelDeclarations.size)
            assertTrue(tu.toplevelDeclarations[0] is Declaration.Composite)
            assertEquals(StructOrUnion.Struct, (tu.toplevelDeclarations[0] as Declaration.Composite).structOrUnion)
        }
    }

    @Test
    fun test007() {
        parsed("/parser-tests/007-named-struct-with-var.c") { tu ->
            assertEquals(3, tu.toplevelDeclarations.size)
            assertTrue(tu.toplevelDeclarations[0] is Declaration.Composite)

            assertTrue(tu.toplevelDeclarations[1] is Declaration.Var)
            val book = tu.toplevelDeclarations[1] as Declaration.Var
//            assertEquals(Type.Struct("Book"), book.type)

            assertTrue(tu.toplevelDeclarations[2] is Declaration.Var)
            val another = tu.toplevelDeclarations[2] as Declaration.Var
//            assertEquals(Type.Struct("Book"), another.type)

        }
    }

    @Test
    fun test008() {
        parsed("/parser-tests/008-typedef.c") { tu ->
            assertEquals(3, tu.toplevelDeclarations.size)
            assertTrue(tu.toplevelDeclarations[0] is Declaration.Composite)
            val mybook:  Declaration.Typedef = assertIs(tu.toplevelDeclarations[1])

            assertTrue(tu.toplevelDeclarations[2] is Declaration.Var)
            val another = tu.toplevelDeclarations[2] as Declaration.Var
//            assertEquals(Type.Typedeffed(TypedefRef("mybook", mybook)), another.type)
        }
    }

    /* disabled annotation doesn't work with maven surefire?
    @Disabled
    @Test
    fun test009() {
        parsed("/parser-tests/009-fun-with-doc.c") { tu ->
            // FIXME
            println(tu)
            fail()
        }
    }*/

    @Test
    fun test010() {
        parsed("/parser-tests/010-demo-functions.c") { tu ->
            val fs = tu.toplevelDeclarations
                .functions
                .definitions
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
    /*
    @Disabled("Removed getSource from unit")
    @Test
    fun test011() {
        parsed("/parser-tests/011-preprocessed-demo.c") { tu1 ->
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

                // FIXME
                // assertEquals(unit1.getSource(fn1), unit2.getSource(fn2))
            }
        }
    }*/

    @Test
    fun test012() {
        parsed("/parser-tests/012-line-directive.c") { tu ->
            val fn = tu.toplevelDeclarations[0]
            when (val loc = fn.meta.presumedLocation) {
                is None -> fail()
                is Some -> {
                    assertEquals(Path.of("haha.c"), loc.value.sourceFile)
                    assertEquals(42, loc.value.row)
                }
            }

            val gn = tu.toplevelDeclarations[1]
            when (val loc = gn.meta.presumedLocation) {
                is None -> fail()
                is Some -> {
                    assertEquals(Path.of("werktook.c"), loc.value.sourceFile)
                    assertEquals(19, loc.value.row)
                }
            }
        }
    }

    @Disabled("Removed UnitState.getSource")
    @Test
    fun test013() {
        parsed("/parser-tests/013-cpp-define.c") { tu ->
            val ds = tu.toplevelDeclarations
            assertEquals(1, ds.size)
            val fn = ds[0]

            // FIXME
            // val source = unit.getSource(fn).getOrElse { "" }
            // assertFalse("DELEIPEMACRO".toRegex().containsMatchIn(source))
        }
    }

    @Test
    @Disabled("Removed UnitState.getSource")
    fun test014() {
        parsed("/parser-tests/014-cpp-ifdef.c") { tu  ->
            val ds = tu.toplevelDeclarations
            assertEquals(1, ds.size)
            val fn = ds[0]

//            val source = unit.getSource(fn).getOrElse { "" }
//            assertFalse("bad".toRegex().containsMatchIn(source))
//            println(fn.meta.location)
//            println(fn.meta.presumedLocation)
        }
    }

    @Test
    fun test016() {
        parsed("/parser-tests/016-global-with-initializer.c") { tu ->
            val ds = tu.toplevelDeclarations
            assertEquals(1, ds.size)
            val x = ds[0]

            println(x)
        }
    }

    @Test
    fun test017() {
        parsed("/parser-tests/017-global-with-array-initializer.c") { tu ->
            val ds = tu.toplevelDeclarations
            assertEquals(1, ds.size)
            val x = ds[0]

            println(x)
        }
    }

    @Test
    fun test018() {
        parsed("/parser-tests/018-global-with-function-pointers.c") { tu ->
            val ds = tu.toplevelDeclarations

            println(ds.filterIsInstance<Declaration.Typedef>().map { it.underlyingType })
            println(ds)
        }
    }

    @Test
    fun test019() {
        parsed("/parser-tests/019-union.c") { tu ->
            val ds = tu.toplevelDeclarations

            println(ds)
        }
    }

    @Test
    fun test020() {
        parsed("/parser-tests/020-storage-global.c") { tu ->
            val ds = tu.toplevelDeclarations
            assertEquals(Storage.Static, ds[0].storage)
            assertEquals(Storage.Extern, ds[1].storage)
        }
    }

    @Test
    fun test021() {
        fun show(ident: Option<Ident>, type: Type) =
            """
                {
                  name: $ident
                  type: $type,
                  pretty: ${Pretty.declaration(ident.getOrElse { "" }, type)},
                }
            """.trimIndent()

        parsed("/parser-tests/021-weird-types.c") { tu ->
            val ds = tu.toplevelDeclarations
            tu.toplevelDeclarations
                .filterIsInstance<Declaration.Typedef>()
                .forEach { println(show(it.ident, it.underlyingType)) }
            tu.toplevelDeclarations
                .filterIsInstance<Declaration.Fun<*>>()
                .forEach { println(Pretty.prototype(it)) }
        }
    }

    @Test
    fun unhappy01() {
        val file = kotlin.io.path.createTempFile(suffix=".c").apply {
            writeText("""
                int[] f(); // semantic error
            """.trimIndent())
        }
        val result = ClangParser().parse(file.toFile())
        assertIs<Either.Left<*>>(result)
    }

    @Test
    fun testStructType() = literal("""
        struct S {};
        struct S s;
    """.trimIndent()) { tu ->
        val s = assertNotNull(tu.variables[0])
        // assertEquals(Type.Struct("S"), s.type)
    }
}