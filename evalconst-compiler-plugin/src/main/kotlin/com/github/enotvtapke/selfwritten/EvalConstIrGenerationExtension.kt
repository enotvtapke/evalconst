package com.github.enotvtapke.selfwritten

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class EvalConstIrGenerationExtension(private val prefix: String, private val limit: Int) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(AccessorInliner(prefix, limit, moduleFragment), null)
    }
}

private class AccessorInliner(
    private val prefix: String, private val limit: Int, private val moduleFragment: IrModuleFragment
) : IrElementTransformerVoid() {


    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        val callee = expression.symbol.owner

        if (expression.accept(MyIrInterpreterPrefixedFunChecker(prefix), Unit)) {
            val res = expression.accept(BodyEvaluator(prefix, limit), Unit)
            return res
        }

        return expression
    }
}
