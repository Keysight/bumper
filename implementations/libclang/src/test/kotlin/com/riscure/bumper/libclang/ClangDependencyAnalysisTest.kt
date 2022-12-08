package com.riscure.bumper.libclang

import com.riscure.bumper.assertOK
import com.riscure.bumper.ast.EntityKind
import org.junit.jupiter.api.DisplayName
import kotlin.test.*

class ClangDependencyAnalysisTest: LibclangTestBase() {

    @DisplayName("Function with struct local")
    @Test
    fun test00() = bumped("""
        struct S {};
        void f() {
          struct S s;
        }
    """.trimIndent()) { ast, unit ->
        val f = assertNotNull(ast.functions[0])
        val struct = assertNotNull(ast.structs[0])

        val deps = assertNotNull(unit.dependencies.assertOK().dependencies[f.mkSymbol(ast.tuid)])
        assertEquals(1, deps.size)
        assertEquals(struct.mkSymbol(ast.tuid), deps.first())
    }

    @DisplayName("Function with struct parameter")
    @Test
    fun test01() = bumped("""
        struct S {};
        void f(struct S s) {}
    """.trimIndent()) { ast, unit ->
        val f = assertNotNull(ast.functions[0])
        val struct = assertNotNull(ast.structs[0])

        val deps = assertNotNull(unit.dependencies.assertOK().dependencies[f.mkSymbol(ast.tuid)])

        assertEquals(1, deps.size)
        assertEquals(struct.mkSymbol(ast.tuid), deps.first())
    }

    @DisplayName("Function with elaborated and nested struct local")
    @Test
    fun test02() = bumped("""
        struct T {};
        void f(struct S { struct T t; } s) {}
    """.trimIndent()) { ast, unit ->
        val f = assertNotNull(ast.functions[0])
        val struct_S = assertNotNull(ast.structs.find { it.ident == "S" })
        val struct_T = assertNotNull(ast.structs.find { it.ident == "T" })

        val deps1 = assertNotNull(unit.dependencies.assertOK().dependencies[f.mkSymbol(ast.tuid)])
        assertEquals(1, deps1.size)
        assertContains(deps1, struct_S.mkSymbol(ast.tuid))

        val deps2 = assertNotNull(unit.dependencies.assertOK().dependencies[struct_S.mkSymbol(ast.tuid)])
        assertEquals(1, deps2.size)
        assertContains(deps2, struct_T.mkSymbol(ast.tuid))
    }

    @DisplayName("Function with local var of typedeffed type")
    @Test
    fun test04() = bumped("""
        typedef int MyInt;
        void f() {
          while (0) {
            MyInt i;
          }
        }
    """.trimIndent()) { ast, unit ->
        val f = assertNotNull(ast.functions[0])
        val myInt = assertNotNull(ast.typedefs.find { it.ident == "MyInt" })

        val deps1 = assertNotNull(unit.dependencies.assertOK().dependencies[f.mkSymbol(ast.tuid)])
        assertEquals(1, deps1.size)
        assertContains(deps1, myInt.mkSymbol(ast.tuid))
    }

    @DisplayName("Function using two other functions")
    @Test
    fun test05() = bumped("""
        void g();
        void h();
        void f() {
          g();
          h();
        }
        void h() {}
    """.trimIndent()) { ast, unit ->
        val f = assertNotNull(ast.functions.find { it.ident == "f" })
        val deps = assertNotNull(unit.dependencies.assertOK().dependencies[f.mkSymbol(ast.tuid)])
        assertEquals(2, deps.size)
    }

    @DisplayName("Forward-declared struct")
    @Test
    fun test06() = bumped("""
        struct X;
        void f(struct X*);
        struct X { int a; int b; };
    """.trimIndent()) { ast, unit ->
        val f = assertNotNull(ast.functions.find { it.ident == "f" })
        val deps = assertNotNull(unit.dependencies.assertOK().dependencies[f.mkSymbol(ast.tuid)])
        assertEquals(1, deps.size)
        assertEquals(EntityKind.Struct, deps.first().tlid.kind)
    }

    @DisplayName("Typedeffed struct")
    @Test
    fun test07() = bumped("""
        typedef struct { int a; int b; } X;
        void f(X*);
    """.trimIndent()) { ast, unit ->
        val f = assertNotNull(ast.functions.find { it.ident == "f" })
        val deps = assertNotNull(unit.dependencies.assertOK().dependencies[f.mkSymbol(ast.tuid)])
        assertEquals(1, deps.size)
        assertEquals(EntityKind.Typedef, deps.first().tlid.kind)
    }
}