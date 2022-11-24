package com.riscure.bumper

import arrow.core.*
import com.riscure.bumper.ast.*
import com.riscure.dobby.clang.Options
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.parser.Parser
import com.riscure.bumper.parser.UnitState
import org.opentest4j.AssertionFailedError
import java.io.File
import kotlin.io.path.writeText
import kotlin.test.*

interface ParseTestBase<E,S,U: UnitState<E, S>> {
    val frontend: Frontend<E, S, U>
    val parser: Parser<E, S, U> get() = frontend

    fun <E, T> Either<E, T>.assertOK(): T =
        this.getOrHandle { error ->
            if (error is Throwable)
                fail("Expected success, got error", error)
            else
                fail("Expected success, got error: $error")
        }

    fun <T> Option<T>.assertOK(): T =
        this.getOrElse { fail("Expected some, got none") }

    private fun <T> withTemp(program: String, withFile: (file: File) -> T): T {
        val file: File = kotlin.io.path
            .createTempFile(suffix = ".c")
            .apply { writeText(program) }
            .toFile()

        return try {
            withFile(file)
        } finally {
            // Delete this when the JVM quits, so we have time to read it
            // when we pause the JVM during debug
            file.deleteOnExit()
        }
    }

    fun parsed(program: String, whenOk: (ast: TranslationUnit<*, *>) -> Unit) = withTemp(program) { file ->
        parsed(file, whenOk)
    }

    fun parsedAndRoundtrip(program: String, whenOk: (ast: TranslationUnit<*, *>) -> Unit) =
        parsed(program, whenOk).let {
            // TODO improve, now we parse thrice.
            roundtrip(program)
        }

    fun parsed(test: File, whenOk: (ast: TranslationUnit<*, *>) -> Unit): TranslationUnit<E, S> {
        val unit = parser
            .parse(test)
            .assertOK()

        val ast = unit.ast.assertOK()
        whenOk(ast)

        return ast
    }

    fun invalid(program: String) {
        try {
            parsed(program) {}
        } catch (e: AssertionFailedError) { /* test succeeds */
            return // early
        }

        fail("Expected parse error.")
    }

    // Some temporary storage
    val storage: Storage

    fun bumped(
        program: String,
        opts: Options = listOf(),
        whenOk: (ast: TranslationUnit<E, S>, unit: U) -> Unit
    ): Unit = withTemp(program) { file -> bumped(file, opts, whenOk) }

    fun bumped(
        file: File,
        opts: Options = listOf(),
        whenOk: (ast: TranslationUnit<E, S>, unit: U) -> Unit
    ) {
        println("Preprocessed input at: ${frontend.preprocessedAt(file, opts)}")

        val result = frontend
            .process(file, opts)
            .flatMap { it.ast.map { ast -> Pair(ast, it) } }
            .assertOK()

        whenOk(result.first, result.second)
    }

    fun roundtrip(program: String, opts: Options = listOf(), whenOk: (TranslationUnit<E, S>) -> Unit)
    fun roundtrip(program: String, opts: Options = listOf()): Unit = roundtrip(program) {}

    fun eq(s1: Symbol, s2: Symbol) {
        assertEquals(s1.tlid, s2.tlid)
        // units can differ in roundtrip tests
    }

    fun eq(f1: FieldDecls, f2: FieldDecls) {
        f1.zip(f2) { l, r -> eq(l, r) }
    }

    fun eq(f1: Option<FieldDecls>, f2: Option<FieldDecls>) {
        when (f1) {
            is Some -> {
                val v2 = assertIs<Some<FieldDecls>>(f2)
                eq(f1.value, v2.value)
            }
            is None -> assertIs<None>(f2)
        }
    }

    fun eq(d1: Declaration<*,*>, d2: Declaration<*,*>) {
        assertEquals(d1.tlid, d2.tlid)
        assertEquals(d1.isDefinition, d2.isDefinition)
        assertEquals(d1.storage, d2.storage)

        when (d1) {
            is Declaration.Composite -> {
                val c1 = d1
                val c2 = assertIs<Declaration.Composite>(d2)
                assertEquals(c1.structOrUnion, c2.structOrUnion)
                eq(c1.fields, c2.fields)
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
                assertEquals(v1.ident, v2.ident)
                eq(v1.type, v2.type)
            }
        }
    }

    fun eq(f1: Field, f2: Field) {
        assertEquals(f1.name, f2.name)
        assertEquals(f1.bitfield, f2.bitfield)
        eq(f1.type, f2.type)
    }

    fun eq(t1: FieldType, t2: FieldType) {
        assertEquals(t1.attrs, t2.attrs)

        when (t1) {
            is Type.Int               -> assertEquals(t1, t2)
            is Type.Float             -> assertEquals(t1, t2)
            is Type.Void              -> assertEquals(t1, t2)
            is Type.Complex           -> assertEquals(t1, t2)
            is Type.VaList            -> assertIs<Type.VaList>(t2)

            is Type.Array             -> {
                val a2 = assertIs<Type.Array>(t2)
                assertEquals(t1.size, a2.size)
                eq(t1.elementType, a2.elementType)
            }
            is Type.Atomic            -> {
                val a2 = assertIs<Type.Atomic>(t2)
                eq(t1.elementType, a2.elementType)
            }
            is Type.Fun               -> {
                val f2 = assertIs<Type.Fun>(t2)
                eq(t1.returnType, f2.returnType)
                assertEquals(t1.params.size, f2.params.size)
                t1.params.zip(f2.params) { l, r -> eq(l, r) }
                assertEquals(t1.vararg, t2.vararg)
            }
            is Type.Ptr               -> {
                val p2 = assertIs<Type.Ptr>(t2)
                eq(t1.pointeeType, p2.pointeeType)
            }
            is Type.Typedeffed        -> {
                val d2 = assertIs<Type.Typedeffed>(t2)
                eq(t1.ref, d2.ref)
            }
            is Type.Enum              -> {
                val d2 = assertIs<Type.Enum>(t2)
                eq(t1.ref, d2.ref)
            }
            is Type.Struct -> {
                val d2 = assertIs<Type.Struct>(t2)
                eq(t1.ref, d2.ref)
            }
            is Type.Union             -> {
                val u2 = assertIs<Type.Union>(t2)
                eq(t1.ref, u2.ref)
            }
            is FieldType.AnonComposite -> {
                val c2 = assertIs<FieldType.AnonComposite>(t2)
                assertEquals(t1.structOrUnion, c2.structOrUnion)
                eq(t1.fields, c2.fields)
            }
        }
    }

    fun eq(p1: Param, p2: Param) {
        assertEquals(p1.name, p2.name)
        eq(p1.type, p2.type)
    }
}