package com.riscure.bumper.libclang

import com.riscure.bumper.*
import org.bytedeco.llvm.clang.CXCursor
import java.nio.file.Path

open class LibclangTestBase: ParseTestBase<CXCursor, CXCursor, ClangUnitState> {
    override val frontend: Frontend<CXCursor, CXCursor, ClangUnitState>
        get() = frontend(Path.of("clang"), storage)
}

// We initiate the generic test suites from bumper with the libclang implementation of the frontend

class LibclangEnumParseTest     : LibclangTestBase(), EnumParseTest<CXCursor, CXCursor, ClangUnitState>
class LibclangFunctionParseTest : LibclangTestBase(), FunctionParseTest<CXCursor, CXCursor, ClangUnitState>
class LibclangStructParseTest   : LibclangTestBase(), StructParseTest<CXCursor, CXCursor, ClangUnitState>
class LibclangTypedefParseTest  : LibclangTestBase(), TypedefParseTest<CXCursor, CXCursor, ClangUnitState>
class LibclangTypeRoundtripTest : LibclangTestBase(), TypeRoundtripTest<CXCursor, CXCursor, ClangUnitState>
class LibclangStdHeadersTest    : LibclangTestBase(), StdHeadersTest<CXCursor, CXCursor, ClangUnitState>
