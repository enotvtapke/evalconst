package com.github.enotvtapke

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

object EvalConstConfigurationKeys {
    val PREFIX: CompilerConfigurationKey<String> = CompilerConfigurationKey.create("prefix")

    val EVAL_LIMIT: CompilerConfigurationKey<Int> = CompilerConfigurationKey.create("limit")
}

@OptIn(ExperimentalCompilerApi::class)
//@AutoService(CommandLineProcessor::class)
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
            optionName = "limit",
            valueDescription = "Int",
            description = "Eval limit",
            required = false,
            allowMultipleOccurrences = false
        )
    }

    override val pluginId = "com.github.enotvtapke.evalconst"
    override val pluginOptions = listOf(PREFIX_OPTION, EVAL_LIMIT_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) =
        when (option) {
            PREFIX_OPTION -> configuration.put(EvalConstConfigurationKeys.PREFIX, value)
            EVAL_LIMIT_OPTION -> configuration.put(EvalConstConfigurationKeys.EVAL_LIMIT, value.toInt())
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
}
