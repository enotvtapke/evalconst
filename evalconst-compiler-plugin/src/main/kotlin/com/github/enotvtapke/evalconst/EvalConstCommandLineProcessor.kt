package com.github.enotvtapke.evalconst

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

object EvalConstConfigurationKeys {
    val PREFIX: CompilerConfigurationKey<String> = CompilerConfigurationKey.create("const-fun-prefix")
    val STEP_NUMBER_LIMIT: CompilerConfigurationKey<Int> = CompilerConfigurationKey.create("step-limit")
    val STACK_SIZE_LIMIT: CompilerConfigurationKey<Int> = CompilerConfigurationKey.create("stack-limit")
}

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
class EvalConstCommandLineProcessor : CommandLineProcessor {
    companion object {
        val PREFIX_OPTION = CliOption(
            optionName = "prefix",
            valueDescription = "String",
            description = "Const functions prefix",
            required = false,
            allowMultipleOccurrences = false
        )

        val EVAL_LIMIT_OPTION = CliOption(
            optionName = "step-limit",
            valueDescription = "Int",
            description = "Maximal number if statements that can be evaluated during compile-time const evaluation",
            required = false,
            allowMultipleOccurrences = false
        )

        val STACK_LIMIT_OPTION = CliOption(
            optionName = "stack-limit",
            valueDescription = "Int",
            description = "Maximal stack size during compile-time const evaluation",
            required = false,
            allowMultipleOccurrences = false
        )
    }

    override val pluginId = "com.github.enotvtapke.evalconst-compiler-plugin"
    override val pluginOptions = listOf(PREFIX_OPTION, EVAL_LIMIT_OPTION, STACK_LIMIT_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) =
        when (option) {
            PREFIX_OPTION -> configuration.put(EvalConstConfigurationKeys.PREFIX, value)
            EVAL_LIMIT_OPTION -> configuration.put(EvalConstConfigurationKeys.STEP_NUMBER_LIMIT, value.toInt())
            STACK_LIMIT_OPTION -> configuration.put(EvalConstConfigurationKeys.STACK_SIZE_LIMIT, value.toInt())
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
}
