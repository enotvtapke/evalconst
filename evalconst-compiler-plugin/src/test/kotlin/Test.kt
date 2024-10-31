import com.github.enotvtapke.EvalConstComponentRegistrar
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.Result
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class Test {

    @Test
    fun funTest() {
        val result = compile(readTestFile("fun.kt"))
        val code = result.javaCode("MainKt")
    }

    @Test
    fun variablesTest() {
        val result = compile(readTestFile("variables.kt"))
        val code = result.javaCode("MainKt")
    }

    @Test
    fun constTypesOpsTest() {
        val result = compile(readTestFile("const_types_operations.kt"))
        val code = result.javaCode("MainKt")
    }

    @Test
    fun loopsTest() {
        val result = compile(readTestFile("loops.kt"))
        val code = result.javaCode("MainKt")
        println(code)
    }

    @Test
    fun miscTest() {
        val result = compile(readTestFile("misc.kt"))
        val code = result.javaCode("MainKt")
    }

    @Test
    fun conditionsTest() {
        val result = compile(readTestFile("conditions.kt"))
        val code = result.javaCode("MainKt")
        println(code)
    }

    @Test
    fun nestedFunTest() {
//        println(Int::class. members.find { it.name == "and" }?.call(1, 2))
//        Class.forName("kotlin.Int").getDeclaredMethod("plus", Int::class.java)
        val result = compile(readTestFile("nested_fun.kt"))
        val code = result.javaCode("MainKt")
        println(code)
    }

    @Test
    fun recursionTest() {
        val result = compile(readTestFile("recursion.kt"))
        val code = result.javaCode("MainKt")
        println(code)
    }

    private fun compile(sourceFile: String): Result =
        compile(sourceFile = SourceFile.kotlin("main.kt", sourceFile), EvalConstComponentRegistrar()).also {
            assertEquals(KotlinCompilation.ExitCode.OK, it.exitCode)
        }
}

private val main = SourceFile.kotlin(
    "main.kt", """
            fun main() {
                println(evalLoop(5))
            }

            fun evalLoop(i: Int): Int {
                1 + 1
                return i
            }
        """
)
