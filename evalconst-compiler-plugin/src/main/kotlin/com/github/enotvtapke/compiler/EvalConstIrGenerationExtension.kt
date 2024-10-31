package com.github.enotvtapke.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterConfiguration
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterChecker
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterCheckerData
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterCommonChecker
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.isUnsignedType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
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
                checker = MyIrInterpreterPrefixedFunChecker(prefix),
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
//        this.getClass().getMethod(methodName, param1.class, param2.class, ..);
        body.statements
        return base.visitBody(body, data)
    }
}

data class FunSignatureWithSem (val name: FqName, val argsTypes: List<FqName>, val sem: Function<*>)

fun evalFun(f: FunSignatureWithSem, args: List<Any?>): Any? {
    return when (f.sem) {
        is Function0<*> -> f.sem.invoke()
        is Function1<*, *> -> (f.sem as Function1<Any?, *>).invoke(args[0])
        is Function2<*, *, *> -> (f.sem as Function2<Any?, Any?, *>).invoke(args[0], args[1])
        else -> throw UnsupportedOperationException(f.sem.toString())
    }
}

val builtIns = listOf(
    FunSignatureWithSem(FqName("kotlin.Int.plus"), listOf(FqName("kotlin.Int"), FqName("kotlin.Int"))) { a: Int, b: Int -> a + b },
    FunSignatureWithSem(FqName("kotlin.Int.minus"), listOf(FqName("kotlin.Int"), FqName("kotlin.Int"))) { a: Int, b: Int -> a - b },
)

class MyIrInterpreterPrefixedFunChecker(
    private val prefix: String,
) : IrInterpreterChecker {
    private val callStack = mutableListOf<IrElement>()

    private val allowedMethodsOnPrimitives = setOf(
        "not", "unaryMinus", "unaryPlus", "inv",
        "toString", "toChar", "toByte", "toShort", "toInt", "toLong", "toFloat", "toDouble",
        "equals", "compareTo", "plus", "minus", "times", "div", "rem", "and", "or", "xor", "shl", "shr", "ushr",
        "less", "lessOrEqual", "greater", "greaterOrEqual"
    )
    private val allowedMethodsOnStrings = setOf(
        "<get-length>", "plus", "get", "compareTo", "equals", "toString"
    )

    fun canEvaluateFunction(function: IrFunction): Boolean {
        val returnType = function.returnType
        if (!returnType.isPrimitiveType() && !returnType.isString() && !returnType.isUnsignedType()) return false // TODO remove?

        val parentType = function.parentClassOrNull?.defaultType
        return when {
            parentType == null -> false
            parentType.isPrimitiveType() -> function.name.asString() in allowedMethodsOnPrimitives
            parentType.isString() -> function.name.asString() in allowedMethodsOnStrings
            else -> false
        }
    }

    private inline fun IrElement.asVisited(crossinline block: () -> Boolean): Boolean {
        callStack.add(this)
        return block().also {
            callStack.removeAt(callStack.lastIndex)
        }
    }

    private fun visitStatements(statements: List<IrStatement>, data: IrInterpreterCheckerData): Boolean {
        return statements.all { it.accept(this, data) }
    }

    private fun visitBodyIfNeeded(irFunction: IrFunction, data: IrInterpreterCheckerData): Boolean {
        return irFunction.asVisited { irFunction.body?.accept(this@MyIrInterpreterPrefixedFunChecker, data) ?: true }
    }

    private fun visitValueArguments(expression: IrFunctionAccessExpression, data: IrInterpreterCheckerData): Boolean {
        return (0 until expression.valueArgumentsCount)
            .map { expression.getValueArgument(it) }
            .none { it?.accept(this, data) == false }
    }

    override fun visitConst(expression: IrConst<*>, data: IrInterpreterCheckerData): Boolean {
        return true
    }

    override fun visitVariable(declaration: IrVariable, data: IrInterpreterCheckerData): Boolean {
        return declaration.initializer?.accept(this, data) ?: true
    }

    override fun visitSetValue(expression: IrSetValue, data: IrInterpreterCheckerData): Boolean {
        return expression.value.accept(this, data)
    }

    override fun visitWhen(expression: IrWhen, data: IrInterpreterCheckerData): Boolean {
        if (!data.mode.canEvaluateExpression(expression)) return false

        return expression.branches.all { it.accept(this, data) }
    }

    override fun visitBranch(branch: IrBranch, data: IrInterpreterCheckerData): Boolean {
        return branch.condition.accept(this, data) && branch.result.accept(this, data)
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: IrInterpreterCheckerData): Boolean {
        return loop.asVisited {
            loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: IrInterpreterCheckerData): Boolean {
        return loop.asVisited {
            loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
        }
    }

    override fun visitBreak(jump: IrBreak, data: IrInterpreterCheckerData): Boolean = callStack.contains(jump.loop)

    override fun visitContinue(jump: IrContinue, data: IrInterpreterCheckerData): Boolean = callStack.contains(jump.loop)

    override fun visitCall(expression: IrCall, data: IrInterpreterCheckerData): Boolean {
        if (canEvaluateFunction(expression.symbol.owner)) return true
        if (!expression.symbol.owner.name.asString().startsWith(prefix)) return false // TODO add arg type and return type checks?
        if (expression.symbol.owner in callStack) return true

        return visitValueArguments(expression, data) && visitBodyIfNeeded(expression.symbol.owner, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: IrInterpreterCheckerData): Boolean {
        return expression.symbol.owner.parent in callStack
    }

    override fun visitBody(body: IrBody, data: IrInterpreterCheckerData): Boolean {
        return visitStatements(body.statements, data)
    }

    override fun visitReturn(expression: IrReturn, data: IrInterpreterCheckerData): Boolean {
        if (!callStack.contains(expression.returnTargetSymbol.owner)) return false
        return expression.value.accept(this, data)
    }

    override fun visitElement(
        element: IrElement,
        data: IrInterpreterCheckerData
    ): Boolean = false
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

