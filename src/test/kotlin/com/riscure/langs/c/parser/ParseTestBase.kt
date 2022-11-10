package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.dobby.clang.Options
import com.riscure.langs.c.Frontend
import com.riscure.langs.c.Storage
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.index.Symbol
import com.riscure.langs.c.parser.clang.*
import com.riscure.langs.c.pp.AstWriters
import com.riscure.langs.c.pp.Extractor
import org.bytedeco.llvm.clang.CXCursor
import org.opentest4j.AssertionFailedError
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.*

open class ParseTestBase {

    fun <E, T> Either<E, T>.assertOK(): T =
        this.getOrHandle { error ->
            if (error is Throwable)
                fail("Expected success, got error", error)
            else
                fail("Expected success, got error: $error")
        }

    fun <T> Option<T>.assertOK(): T =
        this.getOrElse { fail("Expected some, got none") }

    private fun withTemp(program: String, withFile: (file: File) -> Unit) {
        val file: File = kotlin.io.path
            .createTempFile(suffix = ".c")
            .apply { writeText(program) }
            .toFile()

        try {
            withFile(file)
        } finally {
            // Delete this when the JVM quits, so we have time to read it
            // when we pause the JVM during debug
            file.deleteOnExit()
        }
    }

    fun parsed(program: String, whenOk: (ast: ErasedTranslationUnit) -> Unit) = withTemp(program) { file ->
        parsed(file, whenOk)
    }

    fun parsed(test: File, whenOk: (ast: ErasedTranslationUnit) -> Unit) {
        val unit = ClangParser()
            .parse(test)
            .assertOK()

        val ast = unit.ast.assertOK()
        whenOk(ast)
    }

    fun invalid(program: String) = try {
        parsed(program) {}

        fail("Expected parse error.")
    } catch (e: AssertionFailedError) { /* test succeeds */
    }

    // Some temporary storage
    val storage = Storage
        .temporary("test-storage")
        .getOrHandle { throw it }

    // initiate the frontend
    val frontend = Frontend
        .clang(Path.of("clang"), storage)

    fun bumped(
        program: String,
        opts: Options = listOf(),
        whenOk: (ast: ClangTranslationUnit, unit: ClangUnitState) -> Unit
    ): Unit = withTemp(program) { file -> bumped(file, opts, whenOk) }

    fun bumped(
        file: File,
        opts: Options = listOf(),
        whenOk: (ast: ClangTranslationUnit, unit: ClangUnitState) -> Unit
    ) {
        println("Preprocessed input at: ${frontend.preprocessedAt(file, opts)}")

        val result = frontend
            .process(file, opts)
            .flatMap { it.ast.map { ast -> Pair(ast, it) } }
            .assertOK()

        whenOk(result.first, result.second)
    }

    object roundtrip {
        fun eq(s1: Symbol, s2: Symbol) {
            assertEquals(s1.tlid, s2.tlid)
            assertEquals(s1.site, s2.site)
            // units can differ in roundtrip tests
        }

        fun eq(d1: ErasedDeclaration, d2: ErasedDeclaration) {
            assertEquals(d1.tlid, d2.tlid)
            assertEquals(d1.site, d2.site)
            assertEquals(d1.isDefinition, d2.isDefinition)
            assertEquals(d1.storage, d2.storage)
            assertEquals(d1.visibility, d2.visibility)

            when (d1) {
                is Declaration.Composite -> {
                    val c1 = d1
                    val c2 = assertIs<Declaration.Composite>(d2)
                    assertEquals(c1.structOrUnion, c2.structOrUnion)
                    assertEquals(c1.fields.isDefined(), c2.fields.isDefined())
                    c1.fields.tap { f1 -> c2.fields.tap { f2 ->
                        f1.zip(f2) { l, r ->
                            eq(l, r)
                        }
                    }}
                }
                is Declaration.Enum      -> {
                    val e1 = d1
                    val e2 = assertIs<Declaration.Enum>(d2)
                    assertEquals(e1.enumerators, e2.enumerators)
                }
                is Declaration.Fun       -> {
                    val f1 = d1
                    val f2 = assertIs<Declaration.Fun<*>>(d2)
                    eq(f1.returnType, f2.returnType)
                    assertEquals(f1.params.size, f2.params.size)
                    f1.params.zip(f2.params) { l, r -> eq(l, r) }
                }
                is Declaration.Typedef   -> {
                    val t1 = d1
                    val t2 = assertIs<Declaration.Typedef>(d2)
                    eq(t1.underlyingType, t2.underlyingType)
                }
                is Declaration.Var       -> {
                    val v1 = d1
                    val v2 = assertIs<Declaration.Var<*>>(d2)
                    assertEquals(v1.name, v2.name)
                    eq(v1.type, v2.type)
                }
            }
        }

        fun eq(f1: Field, f2: Field) {
            assertEquals(f1.name, f2.name)
            assertEquals(f1.bitfield, f2.bitfield)
            eq(f1.type, f2.type)
        }

        fun eq(t1: Type, t2: Type) {
            assertEquals(t1.attrs, t2.attrs)

            when (t1) {
                is Type.Int               -> assertEquals(t1, t2)
                is Type.Float             -> assertEquals(t1, t2)
                is Type.Void              -> assertEquals(t1, t2)
                is Type.Complex           -> assertEquals(t1, t2)

                is Type.Array             -> {
                    val a2 = assertIs<Type.Array>(t2)
                    assertEquals(t1.size, a2.size)
                    eq(t1.elementType, a2.elementType)
                }
                is Type.Atomic            -> {
                    val a2 = assertIs<Type.Atomic>(t2)
                    eq(t1.elementType, a2.elementType)
                }
                is Type.Enum              -> assertEquals(t1, t2)
                is Type.Fun               -> {
                    val f2 = assertIs<Type.Fun>(t2)
                    eq(t1.returnType, f2.returnType)
                    assertEquals(t1.params.size, f2.params.size)
                    t1.params.zip(f2.params) { l, r -> eq(l, r) }
                    assertEquals(t1.vararg, t2.vararg)
                }
                is Type.InlineDeclaration -> {
                    val d2 = assertIs<Type.InlineDeclaration>(t2)
                    eq(t1.declaration, d2.declaration)
                }
                is Type.Ptr               -> {
                    val p2 = assertIs<Type.Ptr>(t2)
                    eq(t1.pointeeType, p2.pointeeType)
                }
                is Type.Struct            -> {
                    val s2 = assertIs<Type.Struct>(t2)
                    eq(t1.ref, s2.ref)
                }
                is Type.Typedeffed        -> {
                    val d2 = assertIs<Type.Typedeffed>(t2)
                    eq(t1.ref, d2.ref)
                }
                is Type.Union             -> {
                    val u2 = assertIs<Type.Union>(t2)
                    eq(t1.ref, u2.ref)
                }
            }
        }

        fun eq(r1: Ref, r2: Ref) {
            assertEquals(r1.byName, r2.byName)
            // TODO, site comparison?
        }

        fun eq(p1: Param, p2: Param) {
            assertEquals(p1.name, p2.name)
            eq(p1.type, p2.type)
        }

    }
}