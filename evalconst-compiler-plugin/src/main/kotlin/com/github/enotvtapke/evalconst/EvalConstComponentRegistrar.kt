package com.github.enotvtapke.evalconst

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class EvalConstComponentRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val prefix = configuration.get(EvalConstConfigurationKeys.PREFIX) ?: "eval"
        val stepLimit = configuration.get(EvalConstConfigurationKeys.STEP_NUMBER_LIMIT) ?: 1_000_000
        val stackSizeLimit = configuration.get(EvalConstConfigurationKeys.STACK_SIZE_LIMIT) ?: 1000
        IrGenerationExtension.registerExtension(EvalConstFunIrGenerationExtension(prefix, stepLimit, stackSizeLimit))
    }
}
