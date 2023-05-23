package com.riscure.bumper

import com.riscure.bumper.ast.Stdlibs
import com.riscure.bumper.ast.show
import com.riscure.bumper.parser.UnitState
import org.junit.jupiter.api.Test

/**
 * Bumper includes a stdlib that encapsulates knowledge about the c standard libraries
 * for different platform toolchans.
 *
 * This test tests if that knowledge actually matches the platform's implementation.
 */
interface BumperStdlibTest<E,S,U: UnitState<E, S, U>> : ParseTestBase<E, S, U> {

    val lib: Stdlibs get() = frontend.stdlib

    @Test
    fun size_t() = bumped(
        """
            #include <stddef.h>
            
            // this should go through if and only if it matches the one in the header
            typedef ${lib.size_t.show()} size_t;
        """.trimIndent()
    ) { _ -> }

    @Test
    fun malloc() = bumped(
        """
            #include <stdio.h>
            ${lib.malloc.show()}
        """.trimIndent()
    ) { _ -> }
}