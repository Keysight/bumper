package com.riscure.bumper.libclang

import arrow.core.*
import com.riscure.bumper.*
import com.riscure.bumper.ast.TranslationUnit
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

    override fun roundtrip(program: String, opts: Options, whenOk: (TranslationUnit<CXCursor, CXCursor>) -> Unit) {
        bumped(program, opts) { ast1, unit1 ->
            val pp1 = unit1.printer.print(ast1).assertOK().write()
            bumped(pp1, opts) { ast2, unit2 ->
                ast1.declarations.zip(ast2.declarations) { l, r ->
                    try {
                        eq(l, r.withMeta(l.meta))
                    } catch (e: Throwable) {
                        println("Pretty 1:\n")
                        println(unit1.printer.print(l).assertOK().write())
                        println("Pretty 2:\n")
                        println(unit1.printer.print(r).assertOK().write())
                        throw e
                    }
                }
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
