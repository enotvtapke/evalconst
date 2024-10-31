package com.github.enotvtapke.selfwritten

import com.github.enotvtapke.selfwritten.evaluator.ConstFunEvaluator
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import kotlin.math.exp

class EvalConstFunIrGenerationExtension(private val prefix: String, private val stepLimit: Int) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(ConstFunTransformer({ it.name.asString().startsWith(prefix) }, stepLimit), null)
    }
}

private class ConstFunTransformer(
    isEvalFunction: (IrFunction) -> Boolean,
    stepLimit: Int,
) : IrElementTransformerVoid() {
    private val checker = ConstFunChecker(isEvalFunction)
    private val evaluator = ConstFunEvaluator(isEvalFunction, stepLimit)

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.accept(checker, Unit)) {
            return expression.accept(evaluator, Unit)
        }
        return super.visitCall(expression)
    }
}
