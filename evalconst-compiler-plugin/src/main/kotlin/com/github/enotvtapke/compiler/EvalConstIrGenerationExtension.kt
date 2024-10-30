package com.github.enotvtapke.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterChecker
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterCheckerData
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterCommonChecker
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import kotlin.math.exp

class EvalConstIrGenerationExtension(private val prefix: String, private val limit: Int) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(MyConstEvaluator(prefix, limit, moduleFragment), null)
    }
}

private class MyConstEvaluator(
    private val prefix: String,
    private val limit: Int,
    moduleFragment: IrModuleFragment,
    private val suppressErrors: Boolean = false
) : IrElementTransformerVoid() {
    private val interpreter: IrInterpreter = IrInterpreter(moduleFragment)

    override fun visitFile(irFile: IrFile): IrFile {
        return irFile.transform(
            IrConstFunTransformer(
                interpreter = interpreter,
                irFile = irFile,
                checker = IrInterpreterPrefixedFunChecker(prefix),
                suppressExceptions = suppressErrors,
                irInterpreterConfiguration = IrInterpreterConfiguration(maxCommands = limit),
            ),
            null
        )
    }
}

class IrInterpreterPrefixedFunChecker(
    private val prefix: String,
    private val base: IrInterpreterChecker = IrInterpreterCommonChecker()
) : IrInterpreterChecker by base {
    override fun visitCall(expression: IrCall, data: IrInterpreterCheckerData): Boolean {
        if (!expression.symbol.owner.name.asString().startsWith(prefix)) return false
        return base.visitCall(expression, data)
    }

    override fun visitBody(body: IrBody, data: IrInterpreterCheckerData): Boolean {
        body.statements
        return base.visitBody(body, data)
    }
}

class IrConstFunTransformer(
    private val interpreter: IrInterpreter,
    private val irFile: IrFile,
    private val checker: IrInterpreterChecker,
    private val mode: EvaluationMode = EvaluationMode.FULL,
    private val suppressExceptions: Boolean = false,
    private val irInterpreterConfiguration: IrInterpreterConfiguration = IrInterpreterConfiguration()
) : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.canBeInterpreted()) {
            return expression.interpret()
        }
        return super.visitCall(expression)
    }

    private fun IrExpression.canBeInterpreted(): Boolean =
        try {
            this.accept(checker, IrInterpreterCheckerData(irFile, mode, interpreter.irBuiltIns, irInterpreterConfiguration))
        } catch (e: Throwable) {
            if (suppressExceptions) false
            throw AssertionError("Error occurred while optimizing an expression:\n${this.dump()}", e)
        }

    private fun IrExpression.interpret(): IrExpression =
        try {
            interpreter.interpret(this, irFile)
        } catch (e: Throwable) {
            if (suppressExceptions) this
            throw AssertionError("Error occurred while optimizing an expression:\n${this.dump()}", e)
        }
}

