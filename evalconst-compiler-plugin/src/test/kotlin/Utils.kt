import com.github.enotvtapke.evalconst.EvalConstComponentRegistrar
import com.strobel.assembler.InputTypeLoader
import com.strobel.assembler.metadata.ArrayTypeLoader
import com.strobel.assembler.metadata.CompositeTypeLoader
import com.strobel.assembler.metadata.ITypeLoader
import com.strobel.decompiler.Decompiler
import com.strobel.decompiler.DecompilerSettings
import com.strobel.decompiler.PlainTextOutput
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.StringWriter
import kotlin.io.path.Path
import kotlin.io.path.readText

@OptIn(ExperimentalCompilerApi::class)
fun compile(
    sourceFiles: List<SourceFile>,
    plugin: CompilerPluginRegistrar = EvalConstComponentRegistrar(),
): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        sources = sourceFiles
        compilerPluginRegistrars = listOf(plugin)
        inheritClassPath = true
    }.compile()
}

@OptIn(ExperimentalCompilerApi::class)
fun compile(
    sourceFile: SourceFile,
    plugin: CompilerPluginRegistrar = EvalConstComponentRegistrar(),
): KotlinCompilation.Result {
    return compile(listOf(sourceFile), plugin)
}

fun readTestFile(filename: String): String = Path("./src/test/kotlin/testSources/$filename").readText()

@OptIn(ExperimentalCompilerApi::class)
fun KotlinCompilation.Result.javaCode(className: String): String {
    val decompilerSettings = DecompilerSettings.javaDefaults().apply {
        typeLoader = CompositeTypeLoader(*(mutableListOf<ITypeLoader>()
            .apply {
                // Ensure every class is available.
                generatedFiles.forEach {
                    add(ArrayTypeLoader(it.readBytes()))
                }

                // Loads any standard classes already on the classpath.
                add(InputTypeLoader())
            }
            .toTypedArray()))

        isUnicodeOutputEnabled = true
    }

    return StringWriter().let { writer ->
        Decompiler.decompile(
            className,
            PlainTextOutput(writer).apply { isUnicodeOutputEnabled = true },
            decompilerSettings
        )
        writer.toString().trimEnd().trimIndent()
    }
}