package com.riscure.bumper.libclang

import com.riscure.bumper.analyses.LinkAnalysis
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
        val graph = LinkAnalysis.dependencyGraph(units.map { it.first }).assertOK()
        val (unit1, unit2) = units

        assertEquals(2, graph.size)
        val unit1Deps = assertNotNull(graph[unit1.first.tuid])
        val unit2Deps = assertNotNull(graph[unit2.first.tuid])

        assertEquals(1, unit1Deps.size)
        val gDep = unit1Deps.first()
        assertEquals(unit2.first.tuid, gDep.unit)
        assertEquals(unit2.first.functions.find { it.ident == "g"}!!, gDep.needs)

        assertEquals(1, unit2Deps.size)
        val fDep = unit2Deps.first()
        assertEquals(unit1.first.tuid, fDep.unit)
        assertEquals(unit1.first.functions.find { it.ident == "f"}!!, fDep.needs)
    }

}