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
        run {
            // We test the link graph
            val graph = LinkAnalysis.linkGraph(units.map { it.first }).assertOK()
            val (unit1, unit2) = units

            val unit1Deps = graph[unit1.first.tuid].assertOK()
            val unit2Deps = graph[unit2.first.tuid].assertOK()

            assertEquals(1, unit1Deps.size)
            val gDep = unit1Deps.first()
            assertEquals(unit2.first.tuid, gDep.definition.unit)
            assertEquals(unit2.first.functions.find { it.ident == "g" }!!, gDep.definition.decl)

            assertEquals(1, unit2Deps.size)
            val fDep = unit2Deps.first()
            assertEquals(unit1.first.tuid, fDep.definition.unit)
            assertEquals(unit1.first.functions.find { it.ident == "f" }!!, fDep.definition.decl)
        }

        // We can also look at the complete cross-unit dependency graph
        run {
            val graph = LinkAnalysis.crossUnitDependencyGraph(units.map { it.second }).assertOK()
            val (unit1, unit2) = units

            val fdef  = assertNotNull(unit1.first.functions.find { it.ident == "f" })
            val fdeps = assertNotNull(graph[fdef.mkSymbol(unit1.first.tuid)])

            val gdec     = assertNotNull(unit1.first.functions.find { it.ident == "g" })
            val gdecdeps = assertNotNull(graph[gdec.mkSymbol(unit1.first.tuid)])

            assertEquals(2, fdeps.size)
            assertEquals(1, gdecdeps.size)
        }
    }

}