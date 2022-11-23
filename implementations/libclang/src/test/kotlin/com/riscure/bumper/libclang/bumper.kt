package com.riscure.bumper.libclang

import arrow.core.*
import com.riscure.bumper.*
import com.riscure.bumper.ast.TranslationUnit
import org.bytedeco.llvm.clang.CXCursor
import java.nio.file.Path

open class LibclangTestBase: ParseTestBase<CXCursor, CXCursor, ClangUnitState> {

    override val frontend: Frontend<CXCursor, CXCursor, ClangUnitState>
        get() = frontend(Path.of("clang"), storage)

    override val storage =
        Storage
            .temporary("test-storage")
            .getOrHandle { throw it }

    override fun roundtrip(program: String, whenOk: (TranslationUnit<CXCursor, CXCursor>) -> Unit) {
        bumped(program, listOf()) { ast1, unit1 ->
            val pp1 = unit1.printer.print(ast1).assertOK().write()
            bumped(pp1, listOf()) { ast2, unit2 ->
                ast1.declarations.zip(ast2.declarations) { l, r ->
                    try {
                        eq(l, r.withMeta(l.meta))
                    } catch (e: Throwable) {
                        println("Pretty 1:\n" + unit1.printer.print(l))
                        println("Pretty 2:\n" + unit2.printer.print(r))
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
