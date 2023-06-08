package com.riscure.bumper

import com.riscure.bumper.analyses.typeDependencies
import com.riscure.bumper.ast.EntityKind
import com.riscure.bumper.ast.TranslationUnit
import com.riscure.bumper.ast.TypeContext
import com.riscure.bumper.ast.TypeResolutions
import com.riscure.bumper.parser.UnitState
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

interface TypeDependencyAnalysisTest<E,S,U: UnitState<E, S, U>>: ParseTestBase<E, S, U>  {

    fun testDependencies(
        typedef: String,
        needSize: Boolean = true,
        whenOk: (TypeContext, TranslationUnit<E, S>, TypeResolutions) -> Unit,
    ) = bumped(typedef) { ast, unit ->
        val type = ast.typedefs
            .find { it.ident == "Test" }
            .assertOK()
            .underlyingType

        val deps = typeDependencies(type, needSize).assertOK()

        // extract the type environment, and use it to resolve the dependencies.
        val env = ast.typeEnv(builtins)
        val resolution = env.resolve(deps).assertOK()

        whenOk(deps, ast, resolution)
    }

    @Test
    fun `int-pointer-without-dependencies`() = testDependencies("""
        typedef int *Test;
    """.trimIndent()) { deps, _, _ ->
        assertTrue(deps.get().isEmpty())
    }

    @Test
    fun `struct-dependency`() = testDependencies("""
        struct A { int i; };
        typedef struct A Test;
    """.trimIndent()) { deps, ast, res->
        val ds = deps.get()
        val typ = ds.keys.first()

        assertEquals("A", typ.ref.name)
        assertEquals(EntityKind.Struct, typ.kind)
        assertTrue(ds[typ].assertOK())

        val struct = ast.structs.first().assertOK()
        assertEquals(struct, res[typ])
    }

    @Test
    fun `struct-pointer-dependency`() = testDependencies("""
        struct A { int i; };
        typedef struct A *Test;
    """.trimIndent()) { deps, ast, res ->
        val ds = deps.get()
        val typ = ds.keys.first()

        assertEquals("A", typ.ref.name)
        assertEquals(EntityKind.Struct, typ.kind)
        assertFalse(ds[typ].assertOK())

        val struct = ast.structs.first().assertOK()
        assertEquals(struct, res[typ])
    }

    @Test
    fun `function-pointer-dependency`() = testDependencies("""
        struct A { int i; };
        typedef void (*Test)(int i, struct A a);
    """.trimIndent()) { deps, ast, res ->
        val ds = deps.get()
        val typ = ds.keys.first()

        assertEquals("A", typ.ref.name)
        assertEquals(EntityKind.Struct, typ.kind)
        assertFalse(ds[typ].assertOK())

        // even though we don't need a full definition,
        // we get it as the only available thing
        val struct = ast.structs.first().assertOK()
        assertEquals(struct, res[typ])
    }

    @Test
    fun `function-pointer-dependency-only-decl`() = testDependencies("""
        struct A;
        typedef void (*Test)(int i, struct A a);
    """.trimIndent()) { deps, ast, res ->
        val ds = deps.get()
        val typ = ds.keys.first()

        assertEquals("A", typ.ref.name)
        assertEquals(EntityKind.Struct, typ.kind)
        assertFalse(ds[typ].assertOK())

        val struct = ast.structs.first().assertOK()
        assertEquals(struct, res[typ])
    }

    @Test
    fun `function-type-dependency`() = testDependencies("""
        struct A {};
        typedef void (Test)(int i, struct A a);
    """.trimIndent()) { deps, ast, res ->
        val ds = deps.get()
        val typ = ds.keys.first()

        assertEquals("A", typ.ref.name)
        assertEquals(EntityKind.Struct, typ.kind)
        assertTrue(ds[typ].assertOK())

        val struct = ast.structs.first().assertOK()
        assertEquals(struct, res[typ])
    }

    @Test
    fun `function-type-dependency-without-size`() = testDependencies("""
        struct A;
        typedef void (Test)(int i, struct A a);
    """.trimIndent(), needSize = false) { deps, ast, res ->
        // we passed needSize = false, indicating
        // that we are asking for dependencies for a type that we do not need to know the size of.
        // this should propagate to the dependencies.
        val ds = deps.get()
        val typ = ds.keys.first()

        assertEquals("A", typ.ref.name)
        assertEquals(EntityKind.Struct, typ.kind)
        assertFalse(ds[typ].assertOK())

        val struct = ast.structs.first().assertOK()
        assertEquals(struct, res[typ])
    }

    @Test
    fun `need-size-overrides`() = testDependencies("""
        struct A { int i; };
        typedef void (Test)(struct A*, struct A, struct A*);
    """.trimIndent()) { deps, ast, res ->
        val ds = deps.get()
        val typ = ds.keys.find { it.ref.name == "A" }.assertOK()
        assertEquals(EntityKind.Struct, typ.kind)
        assertTrue(ds[typ].assertOK())

        val struct = ast.structs.first().assertOK()
        assertEquals(struct, res[typ])
    }

    @Test
    fun `resolve-prefers-definition`() = testDependencies("""
        struct A;
        typedef void (Test)(struct A*);
        struct A { int i; };
    """.trimIndent()) { deps, ast, res ->
        val ds = deps.get()
        val typ = ds.keys.find { it.ref.name == "A" }.assertOK()
        assertEquals(EntityKind.Struct, typ.kind)
        // we don't need a definition
        assertFalse(ds[typ].assertOK())

        // but we get it, because it is available
        val struct = ast.structs
            .find { it.ident == "A" && it.isDefinition }
            .assertOK()
        assertEquals(struct, res[typ])
    }
}