package com.riscure.bumper.libclang

import arrow.core.getOrHandle
import com.riscure.bumper.*
import com.riscure.bumper.ast.TranslationUnit
import com.riscure.bumper.pp.SourceExtractor
import com.riscure.dobby.clang.Options
import org.bytedeco.llvm.clang.CXCursor
import java.nio.file.Path

open class LibclangTestBase: ParseTestBase<CXCursor, CXCursor, ClangUnitState> {

    override val frontend: Frontend<CXCursor, CXCursor, ClangUnitState>
        get() = frontend(Path.of("clang"), storage)

    override val storage =
        Storage
            .temporary("test-storage")
            .getOrHandle { throw it }

    override fun roundtrip(program: Path, opts: Options, whenOk: (TranslationUnit<CXCursor, CXCursor>, ClangUnitState) -> Unit) {
        // parse
        val (cpp1, unit1) = process(program, opts)
        val ast1 = unit1.use { it.ast }

        // pretty print
        val pp1 = ClangUnitState
            .pp(SourceExtractor(cpp1))
            .print(ast1)
            .assertOK()
            .writeTo()

        // now we go again:
        // parse the pretty-printed result
        val (cpp2, unit2) = process(program, opts)
        val ast2 = unit2.use { it.ast }

        // assert the roundtrip equality property
        ast1.declarations.zip(ast2.declarations) { l, r ->
            try {
                eq(l, r.withMeta(l.meta))
            } catch (e: Throwable) {
                println("Expected parsed declarations to be structurally equal, but they weren't.")

                // give a nice error comparing the pretty-printed versions of the two models
                println("Pretty left:\n")
                println(ClangUnitState.pp(SourceExtractor(cpp1)).print(l).assertOK().writeTo())
                println("Pretty right:\n")
                println(ClangUnitState.pp(SourceExtractor(cpp2)).print(r).assertOK().writeTo())

                // rethrow the assertion error
                throw e
            }
        }
    }
}

// We initiate the generic test suites from bumper with the libclang implementation of the frontend

class LibclangEnumParseTest     : LibclangTestBase(), EnumParseTest<CXCursor, CXCursor, ClangUnitState>
class LibclangFunctionParseTest : LibclangTestBase(), FunctionParseTest<CXCursor, CXCursor, ClangUnitState>
class LibclangStructParseTest   : LibclangTestBase(), StructParseTest<CXCursor, CXCursor, ClangUnitState>
class LibclangTypedefParseTest  : LibclangTestBase(), TypedefParseTest<CXCursor, CXCursor, ClangUnitState>
class LibclangTypeRoundtripTest : LibclangTestBase(), TypeRoundtripTest<CXCursor, CXCursor, ClangUnitState>
class LibclangStdHeadersTest    : LibclangTestBase(), StdHeadersTest<CXCursor, CXCursor, ClangUnitState>
class LibclangGlobalParseTest   : LibclangTestBase(), GlobalParseTest<CXCursor, CXCursor, ClangUnitState>
class LibclangTypeTest          : LibclangTestBase(), TypeTest<CXCursor, CXCursor, ClangUnitState>
class LibclangIndexTest         : LibclangTestBase(), IndexTest<CXCursor, CXCursor, ClangUnitState>
