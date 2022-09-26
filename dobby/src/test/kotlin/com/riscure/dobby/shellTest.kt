package com.riscure.dobby

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ShellTest {

    @Test
    fun lexShellWsEscape() {
        val result: List<String> = Shell.line("x\\ y")
        assertEquals(1, result.size)
        assertEquals("x\\ y", result[0])
    }


    @Test
    fun lexShellWsEscapedEscape() {
        val result: List<String> = Shell.line("x\\\\ y")
        assertEquals(2, result.size)
        assertEquals("x\\\\", result[0])
        assertEquals("y", result[1])
    }

    @Test
    fun shellParse01() {
        val result: List<String> =
            Shell.line("C:\\PROGRA~1\\LLVM\\bin\\clang.exe -DENGINESDIR=\"C:/Program Files (x86)/openssl/engines-1.1\" -DOPENSSLDIR=\"C:/Program Files (x86)/openssl/ssl\" -DOPENSSL_NO_ASM -DOPENSSL_NO_STATIC_ENGINE -DOPENSSL_SYSNAME_WIN32 -DWIN32_LEAN_AND_MEAN -D_CRT_SECURE_NO_WARNINGS -isystem C:/Users/JohnDoe/Documents/openssl-cmake -isystem C:/Users/JohnDoe/Documents/openssl-cmake/build/include -isystem C:/Users/JohnDoe/Documents/openssl-cmake/build/crypto -isystem C:/Users/JohnDoe/Documents/openssl-cmake/crypto/ec/curve448/arch_32 -isystem C:/Users/JohnDoe/Documents/openssl-cmake/crypto/ec/curve448 -isystem C:/Users/JohnDoe/Documents/openssl-cmake/crypto/modes -g -Xclang -gcodeview -O0 -D_DEBUG -D_DLL -D_MT -Xclang --dependent-lib=msvcrtd -o crypto\\CMakeFiles\\crypto.dir\\cpt_err.c.obj -c C:\\Users\\JohnDoe\\Documents\\openssl-cmake\\crypto\\cpt_err.c")

        assertEquals(33, result.size)
        assertEquals("C:\\PROGRA~1\\LLVM\\bin\\clang.exe", result[0])
        assertEquals("-DENGINESDIR=\"C:/Program Files (x86)/openssl/engines-1.1\"", result[1])
        assertEquals("-DOPENSSLDIR=\"C:/Program Files (x86)/openssl/ssl\"", result[2])
        assertEquals("-DOPENSSL_NO_ASM", result[3])
        assertEquals("-DOPENSSL_NO_STATIC_ENGINE", result[4])
        assertEquals("-DOPENSSL_SYSNAME_WIN32", result[5])
        assertEquals("-DWIN32_LEAN_AND_MEAN", result[6])
        assertEquals("-D_CRT_SECURE_NO_WARNINGS", result[7])
        assertEquals("-isystem", result[8])
        assertEquals("C:/Users/JohnDoe/Documents/openssl-cmake", result[9])
        assertEquals("-isystem", result[10])
        assertEquals("C:/Users/JohnDoe/Documents/openssl-cmake/build/include", result[11])
        assertEquals("-isystem", result[12])
        assertEquals("C:/Users/JohnDoe/Documents/openssl-cmake/build/crypto", result[13])
        assertEquals("-isystem", result[14])
        assertEquals("C:/Users/JohnDoe/Documents/openssl-cmake/crypto/ec/curve448/arch_32", result[15])
        assertEquals("-isystem", result[16])
        assertEquals("C:/Users/JohnDoe/Documents/openssl-cmake/crypto/ec/curve448", result[17])
        assertEquals("-isystem", result[18])
        assertEquals("C:/Users/JohnDoe/Documents/openssl-cmake/crypto/modes", result[19])
        assertEquals("-g", result[20])
        assertEquals("-Xclang", result[21])
        assertEquals("-gcodeview", result[22])
        assertEquals("-O0", result[23])
        assertEquals("-D_DEBUG", result[24])
        assertEquals("-D_DLL", result[25])
        assertEquals("-D_MT", result[26])
        assertEquals("-Xclang", result[27])
        assertEquals("--dependent-lib=msvcrtd", result[28])
        assertEquals("-o", result[29])
        assertEquals("crypto\\CMakeFiles\\crypto.dir\\cpt_err.c.obj", result[30])
        assertEquals("-c", result[31])
        assertEquals("C:\\Users\\JohnDoe\\Documents\\openssl-cmake\\crypto\\cpt_err.c", result[32])
    }
}