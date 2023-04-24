package com.riscure.bumper.libclang

import com.riscure.bumper.analyses.LinkAnalysis
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
        run {
            // We test the link graph
            val graph = LinkAnalysis(units.map { it.first }).assertOK()
            val (unit1, unit2) = units

            val unit1Deps = graph[unit1.first.tuid].assertOK()
            val unit2Deps = graph[unit2.first.tuid].assertOK()

            assertEquals(1, unit1Deps.bound.size)
            val gDep = unit1Deps.bound.first()
            assertEquals(unit2.first.tuid, gDep.definition.tuid)
            assertEquals(unit2.first.functions.find { it.ident == "g" }.assertOK().prototype, gDep.definition.proto)

            assertEquals(1, unit2Deps.bound.size)
            val fDep = unit2Deps.bound.first()
            assertEquals(unit1.first.tuid, fDep.definition.tuid)
            assertEquals(unit1.first.functions.find { it.ident == "f" }.assertOK().prototype, fDep.definition.proto)
        }
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
        run {
            val graph = LinkAnalysis(units.map { it.first }).assertOK()
            val (unit1, unit2) = units

            val objectInterface1 = LinkAnalysis.objectInterface(unit1.first)
            val objectInterface2 = LinkAnalysis.objectInterface(unit2.first)

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
    }

    /** See test03 */
    @Test
    fun test02() = invalid(
            """
            void f(int);
            void g() { printf("general kenobi"); } // implicit declaration
            """.trimIndent(),
            """
            void f(int x) {}
            """.trimIndent()

    ) /* { units ->
        run {
            val graph = LinkAnalysis.linkGraph(units.map { it.first }).assertOK()
            val (unit1, _) = units
            val unit1Deps = graph[unit1.first.tuid].assertOK()
            assertEquals(1, unit1Deps.size)
            val edge = unit1Deps.first()
            assertNotNull(edge.definition)
            assertEquals("f", edge.definition.decl.ident)
        }
    }*/

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
            val graph = LinkAnalysis.linkGraph(units.map { it.first }).assertOK()
            val (unit1, unit2) = units
            val unit1Deps = graph[unit1.first.tuid].assertOK()
            val unit2Deps = graph[unit2.first.tuid].assertOK()
            assertEquals(1, unit1Deps.size)
            assertEquals(0, unit2Deps.size)
        }
    } */

// The linkgraph only contains external dependencies,
// whereas this test tries to query a graph presupposing it also contains internal dependencies.
// needs fixing.
//    @Test
//    fun test04() = bumped(
//            """
//            b();
//            void a() { b(); }
//            """.trimIndent(),
//            """
//            c();
//            void b() { c(); }
//            """.trimIndent(),
//            """
//            void c() {}
//            """.trimIndent()
//
//    ) { units ->
//        run {
//            val graph = LinkAnalysis.linkGraph(units.map { it.first }).assertOK()
//            val (unit1, unit2, unit3) = units
//            val unit1Deps = graph[unit1.first.tuid].assertOK()
//            val unit2Deps = graph[unit2.first.tuid].assertOK()
//            val unit3Deps = graph[unit3.first.tuid].assertOK()
//            assertEquals(1, unit1Deps.size)
//            assertEquals(1, unit2Deps.size)
//            assertEquals(0, unit3Deps.size)
//            val a = unit1.first.functions.find { it.ident == "a" }!!.mkSymbol(unit1.first.tuid)
//            val reachable = graph.externalDependencyGraph.reachable(setOf(a))
//            println(reachable)
//            // `c` is reachable from calling `b`.
//            // `c` is also reachable from calling `a` (because `a` calls `b`).
//            // `c` is also reachable by itself (by calling `c`).
//            assertEquals(3, reachable.size)
//        }
//    }
}