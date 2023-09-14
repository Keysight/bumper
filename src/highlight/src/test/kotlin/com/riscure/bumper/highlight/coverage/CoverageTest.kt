package com.riscure.bumper.highlight.coverage

import arrow.core.Either
import com.riscure.bumper.coverage.Report
import com.riscure.bumper.highlight.annotate
import com.riscure.bumper.highlight.tokenize
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CoverageTest {

    @Test
    fun `deserialize-clang-coverage-json`() {
        val data = javaClass.classLoader.getResource("fuzz.coverage.json")!!.readText()
        val element = Json.parseToJsonElement(data)
        val result = Report.deserialize(element)

        val report = assertIs<Either.Right<Report>>(result).value
        assertEquals(1, report.files.size)
        val file = report.files[0]
        assertEquals(25, file.segments.size)

        // lex the source file that belongs with it
        val cfile = javaClass.classLoader.getResource("fuzz.c").openStream().reader()
        val toks  = tokenize(cfile)
        val annotated = annotate(toks, file.segments.map { it.marker })
        println(annotated)
    }
}