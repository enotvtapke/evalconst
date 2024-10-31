package com.github.enotvtapke.evalconst

import com.github.enotvtapke.evalconst.evaluator.ConstFunEvaluator
import com.github.enotvtapke.evalconst.evaluator.EvaluatorStackSizeException
import com.github.enotvtapke.evalconst.evaluator.EvaluatorStatementsLimitException
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class EvalConstFunIrGenerationExtension(private val prefix: String, private val stepLimit: Int, private val stackSizeLimit: Int) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(ConstFunTransformer({ it.name.asString().startsWith(prefix) }, stepLimit, stackSizeLimit), null)
    }
}

private class ConstFunTransformer(
    isEvalFunction: (IrFunction) -> Boolean,
    stepLimit: Int,
    stackSizeLimit: Int,
) : IrElementTransformerVoid() {
    private val checker = ConstFunChecker(isEvalFunction)
    private val evaluator = ConstFunEvaluator(isEvalFunction, stepLimit, stackSizeLimit)

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.accept(checker, Unit)) {
            return try {
                expression.accept(evaluator, Unit)
            } catch (e: Exception) {
                when (e) {
                    is EvaluatorStatementsLimitException, is EvaluatorStackSizeException -> super.visitCall(expression)
                    else -> throw e
                }
                super.visitCall(expression)
            }
        }
        return super.visitCall(expression)
    }
}
