package dobby.clang

import com.riscure.dobby.clang.cli.Spec
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertContains

class CliTest {

    @Test
    fun clang11Test() {
        println(Spec.clang11.aliasing)
        assertContains(Spec.clang11.aliasing["mcpu_EQ"]!!.toList(), "mv5")
    }

}