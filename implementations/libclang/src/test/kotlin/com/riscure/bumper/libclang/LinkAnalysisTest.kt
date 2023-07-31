package com.riscure.bumper.libclang

import com.riscure.bumper.analyses.LinkResolution.Companion.linkgraph
import com.riscure.bumper.analyses.objectInterface
import com.riscure.bumper.assertOK

import org.junit.jupiter.api.DisplayName
import kotlin.test.*

class LinkAnalysisTest: LibclangTestBase() {

    @DisplayName("Two mutually dependent translation units")
    @Test
    fun test00() = bumped(
        """
        struct S {};
        void g(); // dependency
        void h();
        void f() {
          g();
          h();
        }
        void h() {}
        """.trimIndent(),

        """
        static void h() {}
        void f(); // dependency
        void g() {
          f();
        }
        """.trimIndent()

    ) { units ->
        // We test the link graph
        val graph = linkgraph(units.asSequence().map { it.first }).assertOK()
        val (unit1, unit2) = units

        val unit1Deps = graph[unit1.first.tuid].assertOK()
        val unit2Deps = graph[unit2.first.tuid].assertOK()

        assertEquals(1, unit1Deps.bound.size)
        val gDep = unit1Deps.bound.first()
        assertEquals(unit2.first.tuid, gDep.definition.tuid)
        eq(unit2.first.functions.find { it.ident == "g" }.assertOK().prototype, gDep.definition.proto.prototype)

        assertEquals(1, unit2Deps.bound.size)
        val fDep = unit2Deps.bound.first()
        assertEquals(unit1.first.tuid, fDep.definition.tuid)
        eq(unit1.first.functions.find { it.ident == "f" }.assertOK().prototype, fDep.definition.proto.prototype)
    }

    @Test
    fun test01() = bumped(
            """
            void f(int);
            static void h() { f(42); }
            """.trimIndent(),
            """
            void f(int x) {}
            """.trimIndent()

    ) { units ->
        val graph = linkgraph(units.map { it.first }).assertOK()
        val (unit1, unit2) = units

        val objectInterface1 = objectInterface(unit1.first)
        val objectInterface2 = objectInterface(unit2.first)

        assertEquals(1, objectInterface1.imports.size)
        assertEquals("f", objectInterface1.imports.first().ident)
        assertEquals(0, objectInterface1.exports.size)

        assertEquals(0, objectInterface2.imports.size)
        assertEquals(1, objectInterface2.exports.size)
        assertEquals("f", objectInterface2.exports.first().ident)

        val unit1Deps = graph[unit1.first.tuid].assertOK()
        val unit2Deps = graph[unit2.first.tuid].assertOK()

        assertEquals(0, unit2Deps.bound.size)
        assertEquals(1, unit1Deps.bound.size)
        val edge = unit1Deps.bound.first()
        assertNotNull(edge.definition)
        assertEquals("f", edge.definition.proto.tlid.name)
    }

    // check that extern global is found to be linked to global with the same name in another unit.
    @Test
    fun test02() = bumped(
        """
            extern int myInt;
        """.trimIndent(),
        """
            const int myInt = 42;
        """.trimIndent()
    ) { units ->
        val graph = linkgraph(units.map { it.first }).assertOK()
        val (unit1, unit2) = units

        val (_, u1) = unit1
        val linking1 = graph[u1.tuid].assertOK()
        assertEquals(1, linking1.bound.size)
    }

    /**
     * This actually goes through clang usually and links
     * the implicit declaration of f with the definition of f.
     *
     * We don't analyze this correctly at the moment, so we escalate
     * the implicit-function-declaration warning to an error in ClangParser,
     * such that this input is rejected.
     *
     * Better would be to fix the analysis.
     */
    @Test
    @DisplayName("Implicit function declarations")
    fun test03() = invalid(
            """
            void g() { f("general kenobi"); } // implicit declaration
            """.trimIndent(),
            """
            void f(const char*) {}
            """.trimIndent()

    ) /* { units ->
        run {
            val graph = linkgraph.linkGraph(units.map { it.first }).assertOK()
            val (unit1, unit2) = units
            val unit1Deps = graph[unit1.first.tuid].assertOK()
            val unit2Deps = graph[unit2.first.tuid].assertOK()
            assertEquals(1, unit1Deps.size)
            assertEquals(0, unit2Deps.size)
        }
    } */

    @DisplayName("Global variable without initialization is a definition")
    @Test
    fun test04() = bumped(
        """
        int k;
        """.trimIndent(),

        """
        extern int k;
        int f() {
          return k;
        }
        """.trimIndent()

    ) { units ->
        // We test the link graph
        val graph = linkgraph(units.map { it.first }).assertOK()
        val (unit1, unit2) = units

        val unit1Deps = graph[unit1.first.tuid].assertOK()
        val unit2Deps = graph[unit2.first.tuid].assertOK()

        assertEquals(0, unit1Deps.bound.size)

        assertEquals(1, unit2Deps.bound.size)
        val fDep = unit2Deps.bound.first()
        assertEquals("k", fDep.definition.name)
    }

    @DisplayName("Extern global variable with initialization is a definition")
    @Test
    fun test05() = bumped(
        """
        extern int k = 1;
        """.trimIndent(),

        """
        extern int k;
        int f() {
          return k;
        }
        """.trimIndent()

    ) { units ->
        // We test the link graph
        val graph = linkgraph(units.map { it.first }).assertOK()
        val (unit1, unit2) = units

        val unit1Deps = graph[unit1.first.tuid].assertOK()
        val unit2Deps = graph[unit2.first.tuid].assertOK()

        assertEquals(0, unit1Deps.bound.size)

        assertEquals(1, unit2Deps.bound.size)
        val fDep = unit2Deps.bound.first()
        assertEquals("k", fDep.definition.name)
    }
}