package com.github.enotvtapke.reference

import com.github.enotvtapke.evalconst.EvalConstConfigurationKeys
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class ReferenceEvalConstComponentRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val prefix = configuration.get(EvalConstConfigurationKeys.PREFIX) ?: "eval"
        val stepLimit = configuration.get(EvalConstConfigurationKeys.EVAL_LIMIT) ?: 1_000_000
        IrGenerationExtension.registerExtension(ReferenceEvalConstFunIrGenerationExtension(prefix, stepLimit))
    }
}
