package com.riscure.bumper

import arrow.core.*
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.parser.Parser
import com.riscure.bumper.parser.UnitState
import com.riscure.dobby.clang.*
import org.opentest4j.AssertionFailedError
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.copyTo
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.*

fun <E, T> Either<E, T>.assertOK(): T =
    this.getOrHandle { error ->
        if (error is Throwable)
            fail("Expected success, got error", error)
        else
            fail("Expected success, got error: $error")
    }

fun <T> T?.assertOK(): T = assertNotNull(this)

fun <T> Option<T>.assertOK(): T =
    this.getOrElse { fail("Expected some, got none") }

interface ParseTestBase<E,S,U: UnitState<E, S>> {

    companion object {

        private fun unpackStdHeaders(includeDir: String): Path {
            val temp = createTempDirectory("stdheaders")
            val resource = ParseTestBase::class.java.classLoader.getResource(includeDir)
            FileSystems.newFileSystem(resource?.toURI(), System.getenv()).use { fs ->
                val root = fs.getPath("/")
                Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val destination = temp.resolve(root.relativize(file).toString())
                        destination.parent.toFile().mkdirs()
                        file.copyTo(destination)
                        return FileVisitResult.CONTINUE
                    }
                })
            }
            return temp.resolve(includeDir)
        }

        private val glibc            = unpackStdHeaders("glibc/include")
        private val clangResourceDir = unpackStdHeaders("clang/include")

        val stdopts = ClangParser
            .parseOptions(listOf("-I$glibc", "-I$clangResourceDir", "-nostdinc"))
            .assertOK()
    }

    val frontend: Frontend<E, S, U>
    val parser: Parser<E, S, U> get() = frontend

    fun mkTempCFile(program: String): File = kotlin.io.path
        .createTempFile(suffix = ".c")
        .apply { writeText(program) }
        .toFile()

    private fun <T> withTemp(program: String, withFile: (file: File) -> T): T {
        val file = mkTempCFile(program)
        return try {
            withFile(file)
        } finally {
            // Delete this when the JVM quits, so we have time to read it
            // when we pause the JVM during debug
            file.deleteOnExit()
        }
    }

    private fun <T> withTemp(programs: List<String>, withFiles: (files: List<File>) -> T): T {
        val files = programs.map { mkTempCFile(it) }
        return try {
            withFiles(files)
        } finally {
            for (file in files) {
                file.deleteOnExit()
            }
        }
    }

    fun parsed(program: String, whenOk: (ast: TranslationUnit<*, *>) -> Unit) = withTemp(program) { file ->
        parsed(file, whenOk)
    }

    fun parsedAndRoundtrip(program: String, opts: List<Arg> = listOf(), whenOk: (ast: TranslationUnit<E, S>) -> Unit) =
        parsedAndRoundtrip(program, opts) { ast, _ -> whenOk(ast) }

    fun parsedAndRoundtrip(program: String, opts: List<Arg> = listOf(), whenOk: (ast: TranslationUnit<E, S>, unit: U) -> Unit) =
        bumped(program, opts) { ast, unit ->
            // TODO improve, now we parse thrice.
            roundtrip(program, opts)
            whenOk(ast, unit)
        }

    fun parsed(test: File, whenOk: (ast: TranslationUnit<*, *>) -> Unit): TranslationUnit<E, S> {
        val unit = parser.parse(test).assertOK()
        whenOk(unit.ast)
        return unit.ast
    }

    fun invalid(vararg program: String) {
        try {
            program.forEach { parsed(it) {} }
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
        vararg programs: String,
        opts: Options = listOf(),
        whenOk: (ast: List<Pair<TranslationUnit<E, S>, U>>) -> Unit
    ): Unit = withTemp(programs.toList()) { files -> bumped(files, opts, whenOk) }

    fun bumped(
        file: File,
        opts: Options = listOf(),
        whenOk: (ast: TranslationUnit<E, S>, unit: U) -> Unit
    ) {
        val (ast, unit) = process(file, opts)
        whenOk(ast, unit)
    }

    fun bumped(
            file: Path,
            opts: Options = listOf(),
            whenOk: (ast: TranslationUnit<E, S>, unit: U) -> Unit
    ): Unit = bumped(file.toFile(), opts, whenOk)

    fun bumped(
        files: List<File>,
        opts: Options = listOf(),
        whenOk: (ast: List<Pair<TranslationUnit<E, S>, U>>) -> Unit
    ) = whenOk(files.map { process(it, opts) })

    private fun process(file: File, opts: Options): Pair<TranslationUnit<E, S>, U> {
        println("Preprocessed input at: ${frontend.preprocessedAt(file, opts)}")
        return frontend.process(file, opts).map { Pair(it.ast, it) }.assertOK()
    }

    fun roundtrip(program: String, opts: Options = listOf(), whenOk: (TranslationUnit<E, S>) -> Unit)
    fun roundtrip(program: String, opts: Options = listOf()): Unit = roundtrip(program, opts) {}

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

    fun eq(d1: UnitDeclaration<*,*>, d2: UnitDeclaration<*,*>) {
        assertEquals(d1.tlid, d2.tlid)
        assertEquals(d1.isDefinition, d2.isDefinition)
        assertEquals(d1.storage, d2.storage)

        when (d1) {
            is UnitDeclaration.Composite -> {
                val c1 = d1
                val c2 = assertIs<UnitDeclaration.Composite>(d2)
                assertEquals(c1.structOrUnion, c2.structOrUnion)
                eq(c1.fields, c2.fields)
            }
            is UnitDeclaration.Enum      -> {
                val e1 = d1
                val e2 = assertIs<UnitDeclaration.Enum>(d2)
                when (val enumerators1 = e1.enumerators) {
                    is Some -> {
                        val enumerators2 = assertIs<Some<Enumerators>>(e2.enumerators)
                        enumerators1.value.zip(enumerators2.value) { l, r -> eq(l, r) }
                    }
                    is None -> assertIs<None>(e2)
                }
            }
            is UnitDeclaration.Fun       -> {
                val f1 = d1
                val f2 = assertIs<UnitDeclaration.Fun<*>>(d2)
                eq(f1.returnType, f2.returnType)
                assertEquals(f1.params.size, f2.params.size)
                assertEquals(f1.vararg, f2.vararg)
                f1.params.zip(f2.params) { l, r -> eq(l, r) }
            }
            is UnitDeclaration.Typedef   -> {
                val t1 = d1
                val t2 = assertIs<UnitDeclaration.Typedef>(d2)
                eq(t1.underlyingType, t2.underlyingType)
            }
            is UnitDeclaration.Var       -> {
                val v1 = d1
                val v2 = assertIs<UnitDeclaration.Var<*>>(d2)
                assertEquals(v1.ident, v2.ident)
                eq(v1.type, v2.type)
            }
        }
    }

    fun eq(l: Enumerator, r: Enumerator) {
        assertEquals(l.ident, r.ident)
        assertEquals(l.key, r.key)
        eq(l.enum, r.enum)
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
                assertEquals(t1.ref, d2.ref)
            }
            is Type.Enum              -> {
                val d2 = assertIs<Type.Enum>(t2)
                assertEquals(t1.ref, d2.ref)
            }
            is Type.Struct -> {
                val d2 = assertIs<Type.Struct>(t2)
                assertEquals(t1.ref, d2.ref)
            }
            is Type.Union             -> {
                val u2 = assertIs<Type.Union>(t2)
                assertEquals(t1.ref, u2.ref)
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