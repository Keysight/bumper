package com.riscure.dobby.clang

import kotlin.test.*

internal class ArgTest {

//    fun roundtrip(arg: String) {
//        Sh
//    }

    @Test
    fun shellify() {
        val arg1 = Arg(Spec.clang11["o"], listOf("some/path/to/file.c"))
        println(arg1.shellify())

        val arg2 = Arg(Spec.clang11["D"], listOf("LEIPEMACRO=the value"))
        println(arg2.shellify())
    }
}