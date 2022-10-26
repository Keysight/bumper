package com.riscure.langs.c.pp

import arrow.core.flatMap
import arrow.core.getOrHandle
import com.riscure.langs.c.ast.EntityKind
import com.riscure.langs.c.ast.TopLevel
import com.riscure.langs.c.parser.clang.ClangParser
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Manual roundtrip tests (of the same kind as StdHeadersTest but handcrafted).
 * This tests for internal consistency of parsing and pretty-printing.
 * If the parser always yields the empty translation unit, this test succeeds.
 */
class TypePrinterTest {

    fun parse(input: String): TopLevel {
        val file = kotlin.io.path.createTempFile(suffix = ".c").apply { writeText(input) }.toFile()

        return try {
            // create the parser
            val parser = ClangParser()

            // parse the input
            val ast = parser.parse(file)
                .flatMap { it.ast() }
                .getOrHandle { throw it }

            // get the typedef
            ast.decls[0]!!
        } finally {
            file.delete()
        }
    }

    fun roundtrip(input: String, debug: Boolean = false) {
        println("Roundtrip test for:\n$input")
        val ast1 = parse(input)
        if (debug) println("Pretty:\n" + Pretty.lhs(ast1) + ";")

        val ast2 = parse("${Pretty.lhs(ast1)};")
            .withMeta(ast1.meta) // not the same

        if (debug) println("ASTs:\n- $ast1\n- $ast2" )

        assertEquals(ast1, ast2)
    }

    /* function type */
    @Test
    fun test001() = roundtrip("""
    typedef void f();
    """.trimIndent())

    /* function type returning void pointer */
    @Test
    fun test002() = roundtrip("""
    typedef void *h();
    """.trimIndent())

    /* function pointer with two anonymous arguments*/
    @Test
    fun test003() = roundtrip("""
    typedef void (*g)(int,int);
    """.trimIndent())

   /* function pointer */
   @Test
   fun test004() = roundtrip("""
   typedef void (*f0)();
   """.trimIndent())

   /* array of function pointers */
   @Test
   fun test005() = roundtrip("""
   typedef void (*f1[])();
   """.trimIndent())

    /* pointer ot array of function pointers */
    @Test
    fun test006() = roundtrip("""
    typedef void (*(*f2[1]))();
    """.trimIndent())

    @Test
    fun test007() = roundtrip("""
    typedef void (**f2_alt[1])();
    """.trimIndent())

    /* 2D array of function pointers */
    @Test
    fun test008() = roundtrip("""
    typedef void (*f3[1][2])();
    """.trimIndent())

    /* pointer to 2D array of function pointers */
    @Test
    fun test009() = roundtrip("""
    typedef void (*(*f4[1][2]))();
    """.trimIndent())

    @Test
    fun test010() = roundtrip("""
    typedef void (**f4_alt[1][2])();
    """.trimIndent())

    /* array of function pointers that return an int pointer */
    @Test
    fun test011() = roundtrip("""
    typedef int* (*f5[])();
    """.trimIndent())

    /* function pointer that takes function pointer as argument */
    @Test
    fun test012() = roundtrip("""
    typedef void (*f6)(int (*f)());
    """.trimIndent())

    /* Type of a function with no parameters, returning a pointer to a function with two parameters, returning an int */
    @Test
    fun test013() = roundtrip("""
    typedef int (*broWatVoid())(int, int);
    """.trimIndent())

    /* Same with void argument list indicating no arguments */
    @Test
    fun test014() = roundtrip("""
    typedef int (*broWatVoid(void))(int, int);
    """.trimIndent())

    /* Same with extra parens */
    @Test
    fun test015() = roundtrip("""
    typedef int (*(broWatVoid(void)))(int, int);
    """.trimIndent())

    /* Type of a function with no parameters,
     * returning a pointer to a function with no parameters,
     * returning a pointer to a function with no parameters
     * returning int (this is the last one I promise)
     **/
    @Test
    fun wat() = roundtrip("""
    typedef int (*(*(broWatVoid(void)))(void))(void);
    """.trimIndent())

    /*-------------------------------------------------------------------------------- Struct declarations --*/

    @Test
    fun typedefStructDeclaration() = roundtrip("""
        struct A;
    """.trimIndent())

    @Test
    fun emptyStructDefinition() = roundtrip("""
        struct A {};
    """.trimIndent())

    /*-------------------------------------------------------------------------------- Named structs --*/

    @Test
    fun typedefNamedStructReference() = roundtrip("""
    struct FSID { int __val[2]; };
    typedef struct FSID __fsid_t;
    """.trimIndent())

    @Test
    fun typedefNamedStructInline() = roundtrip("""
    typedef struct MyStruct { int m; } MyStruct;
    """.trimIndent(), true)

    /*-------------------------------------------------------------------------------- Anonymous structs --*/

    /* From ctype.h */
    @Test
    fun typedefAnonymousStruct() = roundtrip("""
    typedef struct { int __val[2]; } __fsid_t;
    """.trimIndent(), true)

    /*-------------------------------------------------------------------------------- Storage classes ---*/

    @Test
    fun extern() = roundtrip("""
    extern void f();
    """.trimIndent())

    /* from assert.h */
    @Test
    fun externAssertFail() = roundtrip("""
        extern void __assert_fail (const char *__assertion, const char *__file, unsigned int __line, const char *__function);
    """.trimIndent())

    /*-------------------------------------------------------------------------------- Attributes -------*/

    @Test
    fun constParamPointee() = roundtrip("""
        void f(const char *x);
    """.trimIndent())

    @Test
    fun constParamPtr() = roundtrip("""
        void f(char * const x);
    """.trimIndent())

    @Test
    fun nonConstCharParam() = roundtrip("""
        void f(char x);
    """.trimIndent())

    @Test
    fun constCharParam() = roundtrip("""
        void f(const char x);
    """.trimIndent())

    /* from tgmath.h */
    @Test
    fun clangOverloadable() = roundtrip("""
        static double __attribute__((__overloadable__)) __tg_promote(int);
    """.trimIndent(), true)

    /* from tgmath.h */
    @Test
    fun aligned() = roundtrip("""
        struct my_struct {
          int a __attribute__ ((aligned(16)));
        };
    """.trimIndent(), true)

    /* from stdatomic.h */
    @Test
    fun alignedMultiple() = roundtrip("""
        typedef struct { __attribute__ ((aligned(8))) long long __clang_max_align_nonce1; __attribute__ ((aligned(16))) long double __clang_max_align_nonce2 } max_align_t;
    """.trimIndent(), true)

    /*-------------------------------------------------------------------------------- Complex nums ------*/

    @Test
    fun complex() = roundtrip("""
        typedef _Complex MyComplex;
    """.trimIndent(), true)

    @Test
    fun floatComplex() = roundtrip("""
        typedef float _Complex MyComplex;
    """.trimIndent())

    @Test
    fun doubleComplex() = roundtrip("""
        typedef double _Complex MyComplex;
    """.trimIndent())

    @Test
    fun longDoubleComplex() = roundtrip("""
        typedef long double _Complex MyComplex;
    """.trimIndent())

}