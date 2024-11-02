package com.github.enotvtapke.reference

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterChecker
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterCheckerData
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterCommonChecker
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class ReferenceEvalConstFunIrGenerationExtension(private val prefix: String, private val limit: Int) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(IrConstFileTransformer(prefix, limit, moduleFragment), null)
    }
}

private class IrConstFileTransformer(
    private val prefix: String,
    private val limit: Int,
    moduleFragment: IrModuleFragment,
    private val suppressErrors: Boolean = false
) : IrElementTransformerVoid() {
    private val interpreter: IrInterpreter = IrInterpreter(moduleFragment)
    private val checker = IrInterpreterCommonCheckerWithCriteria({ it.name.asString().startsWith(prefix) })

    override fun visitFile(irFile: IrFile): IrFile {
        return irFile.transform(
            IrConstTransformer(
                interpreter = interpreter,
                irFile = irFile,
                checker = checker,
                suppressExceptions = suppressErrors,
                irInterpreterConfiguration = IrInterpreterConfiguration(maxCommands = limit),
            ),
            null
        )
    }
}

class IrInterpreterCommonCheckerWithCriteria(
    private val isEvalFunction: (IrFunction) -> Boolean,
    private val base: IrInterpreterChecker = IrInterpreterCommonChecker()
) : IrInterpreterChecker by base {
    override fun visitCall(expression: IrCall, data: IrInterpreterCheckerData): Boolean {
        if (!isEvalFunction(expression.symbol.owner)) return false
        return base.visitCall(expression, data)
    }
}

class IrConstTransformer(
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

