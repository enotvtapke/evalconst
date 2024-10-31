import com.github.enotvtapke.evalconst.EvalConstComponentRegistrar
import com.github.enotvtapke.reference.ReferenceEvalConstComponentRegistrar
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.Result
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class EvalConstExtensionTest {

    @Test
    fun funTest() {
        compareWithReference("fun.kt")
    }

    @Test
    fun variablesTest() {
        compareWithReference("variables.kt")
    }

    @Test
    fun constTypesOpsTest() {
        compareWithReference("const_types_operations.kt")
    }

    @Test
    fun loopsTest() {
        compareWithReference("loops.kt")
    }

    // Reference evaluator does not work here
    @Test
    fun nestedLoopsTest() {
        val result = compile(readTestFile("loops_nested.kt"))
        val code = result.javaCode("MainKt")
        println(code)
    }

    @Test
    fun conditionsTest() {
        compareWithReference("conditions.kt")
    }

    @Test
    fun nestedFunTest() {
        compareWithReference("nested_fun.kt")
    }

    // Reference evaluator does not work here
    @Test
    fun recursionTest() {
        val result = compile(readTestFile("recursion.kt"))
        val code = result.javaCode("MainKt")
        println(code)
    }

    // Reference evaluator does not work here
    @Test
    fun exceedStepLimitTest() {
        val result = compile(readTestFile("huge_loop.kt"))
        val code = result.javaCode("MainKt")
        println(code)
    }

    @Test
    fun shadowingTest() {
        compareWithReference("shadowing.kt")
    }

    private fun compile(sourceFile: String, plugin: CompilerPluginRegistrar = EvalConstComponentRegistrar()): Result =
        compile(sourceFile = SourceFile.kotlin("main.kt", sourceFile), plugin).also {
            assertEquals(KotlinCompilation.ExitCode.OK, it.exitCode)
        }

    private fun compareWithReference(fileName: String) {
        val sourceFile = readTestFile(fileName)
        val res = compile(sourceFile)
        val ref = compile(sourceFile, ReferenceEvalConstComponentRegistrar())
        assertEquals(ref.javaCode("MainKt"), res.javaCode("MainKt"))
    }
}
