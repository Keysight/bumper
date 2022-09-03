package dobby.clang

import com.riscure.dobby.clang.spec.Spec
import com.riscure.dobby.clang.parser.*

import org.junit.jupiter.api.Test
import kotlin.test.*

class CliTest {

    @Test
    fun testAliasing() {
        assertContains(Spec.clang11.aliasing["mcpu_EQ"]!!.toList(), "mv5")
    }

    @Test
    fun testMatching() {
        println(parseClangArg("-objcmt-migrate-all"))
        println(parseClangArg("-objcmt-migrate-al"))
        println(parseClangArg("-o bjcmt-migrate-al"))
    }

}