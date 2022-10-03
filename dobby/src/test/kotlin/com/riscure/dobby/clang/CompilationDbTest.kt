package com.riscure.dobby.clang

import arrow.core.Either
import java.io.File
import java.io.InputStream
import kotlin.test.*

internal class CompilationDbTest {

    private fun parsed(resource: String, whenOk: (ast: CompilationDb) -> Unit) =
        File(CompilationDbTest::class.java.getResource(resource)!!.file).let { file ->
            parsed(file.inputStream().buffered(), whenOk)
        }

    private fun parsed(input: InputStream, whenOk: (ast: CompilationDb) -> Unit) {
        when (val result = CompilationDb.read(input)) {
            is Either.Right -> whenOk(result.value)
            is Either.Left  -> fail("Parsing failed", result.value)
        }
    }

    @Test
    fun minimal() = parsed(
        """ [
        {
          "directory": "/test",
          "command": "clang input.c",
          "file": "input.c"
        }
        ]
        """.trimIndent().byteInputStream()
    ) { db ->
        println(db)
    }


    @Test
    fun backslashes() = parsed(
        """ [
        {
          "directory": "/test",
          "command": "clang C:\\some\\windows\\path\\input.c",
          "file": "C:\\some\\windows\\path\\input.c"
        }
        ]
        """.trimIndent().byteInputStream()
    ) { db ->
        println(db)
    }

    @Test
    fun parseTest01() = parsed("/01.cdb.json") { }

    @Test
    fun parseTest02() = parsed("/02.cdb.json") { }

    @Test
    fun parseTest03() = parsed("/03.cdb.json") { }
}