package com.riscure.langs.C.parser

import com.riscure.langs.c.ClangParser
import com.riscure.tc.codeanalysis.clang.compiler2.loader.CompileCommandMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class ClangParserTest() {
    @Test
    fun parseExamples() {
        val c01 = File(ClangParserTest::class.java.getResource("/examples/ex01.c")!!.file)
        val tu = ClangParser().parse(c01)
        println(tu)
    }
}