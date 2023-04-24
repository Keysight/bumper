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
        bumped(program, opts) { ast1, _ ->
            val pp1 = ClangUnitState.pp(ast1.tuid).print(ast1).assertOK().writeTo()
            bumped(pp1, opts) { ast2, unit2 ->
                ast1.declarations.zip(ast2.declarations) { l, r ->
                    try {
                        eq(l, r.withMeta(l.meta))
                    } catch (e: Throwable) {
                        println("Pretty 1:\n")
                        println(ClangUnitState.pp(ast1.tuid).print(l).assertOK().writeTo())
                        println("Pretty 2:\n")
                        println(ClangUnitState.pp(ast1.tuid).print(r).assertOK().writeTo())
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
class LibclangIndexTest         : LibclangTestBase(), IndexTest<CXCursor, CXCursor, ClangUnitState>
