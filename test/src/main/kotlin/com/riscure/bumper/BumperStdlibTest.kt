package com.riscure.bumper

import com.riscure.bumper.ast.Stdlib
import com.riscure.bumper.ast.show
import com.riscure.bumper.parser.UnitState
import com.riscure.dobby.clang.ClangParser
import org.junit.jupiter.api.Test

/**
 * Bumper includes a stdlib that encapsulates knowledge about the c standard libraries
 * for different platform toolchans.
 *
 * This test tests if that knowledge actually matches the platform's implementation.
 */
interface BumperStdlibTest<E,S,U: UnitState<E, S, U>> : ParseTestBase<E, S, U> {

    val lib: Stdlib get() = frontend.stdlib

    fun test(program: String) = bumped(program, opts =
        ClangParser.parseValidOptions(
            "-Werror",          // to fail on incompatible redeclarations
            "-U_FORTIFY_SOURCE"
        )
    ) { _, _ -> }

    @Test
    fun size_t() = test(
        """
            #include <stddef.h>
            
            // this should go through if and only if it matches the one in the header
            typedef ${lib.size_t.show()} size_t;
        """.trimIndent()
    )

    @Test
    fun malloc() = test(
        """
            #include <stdio.h>
            ${lib.malloc.show()}
        """.trimIndent()
    )
}