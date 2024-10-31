package com.github.enotvtapke.evalconst

import com.github.enotvtapke.evalconst.evaluator.args
import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

class ConstFunChecker(
    private val isEvalFunction: (IrFunction) -> Boolean,
) : IrElementVisitor<Boolean, Unit> {
    private val callStack = mutableListOf<IrElement>()

    override fun visitConst(expression: IrConst<*>, data: Unit): Boolean {
        return true
    }

    override fun visitVariable(declaration: IrVariable, data: Unit): Boolean {
        return declaration.initializer?.accept(this, data) ?: true
    }

    override fun visitSetValue(expression: IrSetValue, data: Unit): Boolean {
        return expression.value.accept(this, data)
    }

    override fun visitWhen(expression: IrWhen, data: Unit): Boolean {
        return expression.branches.all { it.accept(this, data) }
    }

    override fun visitBranch(branch: IrBranch, data: Unit): Boolean {
        return branch.condition.accept(this, data) && branch.result.accept(this, data)
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: Unit): Boolean {
        return loop.visitWith {
            loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Unit): Boolean {
        return loop.visitWith {
            loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
        }
    }

    override fun visitCall(expression: IrCall, data: Unit): Boolean {
        if (!isEvalFunction(expression.symbol.owner) && !isPrimitiveFunction(expression.symbol.owner)) return false
        if (expression.symbol.owner in callStack) return true

        return visitValueArguments(expression, data) && visitBodyIfNeeded(expression.symbol.owner, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: Unit): Boolean {
        return expression.symbol.owner.parent in callStack
    }

    override fun visitBody(body: IrBody, data: Unit): Boolean {
        return visitStatements(body.statements, data)
    }

    override fun visitBlock(expression: IrBlock, data: Unit): Boolean {
        return visitStatements(expression.statements, data)
    }

    override fun visitReturn(expression: IrReturn, data: Unit): Boolean {
        if (!callStack.contains(expression.returnTargetSymbol.owner)) return false
        return expression.value.accept(this, data)
    }

    override fun visitElement(
        element: IrElement,
        data: Unit
    ): Boolean = false

    private inline fun IrElement.visitWith(crossinline block: () -> Boolean): Boolean {
        callStack.add(this)
        return block().also {
            callStack.removeAt(callStack.lastIndex)
        }
    }

    private fun visitStatements(statements: List<IrStatement>, data: Unit): Boolean =
        statements.all { it.accept(this, data) }

    private fun visitBodyIfNeeded(irFunction: IrFunction, data: Unit): Boolean =
        irFunction.visitWith { irFunction.body?.accept(this@ConstFunChecker, data) ?: true }

    private fun visitValueArguments(expression: IrFunctionAccessExpression, data: Unit): Boolean =
        expression.args().none { it?.accept(this, data) == false }

    private fun isPrimitiveFunction(function: IrFunction): Boolean {
        val parentType = function.parentClassOrNull?.defaultType
        val fName = function.name.asString()
        return when {
            parentType == null -> function.fqNameWhenAvailable?.asString() in allowedBuiltinExtensionFunctions
            parentType.isPrimitiveType() -> fName in allowedMethodsOnPrimitives
            parentType.isString() -> fName in allowedMethodsOnStrings
            else -> false
        }
    }

    companion object {
        private val allowedMethodsOnPrimitives = setOf(
            "not", "unaryMinus", "unaryPlus", "inv",
            "toString", "toChar", "toByte", "toShort", "toInt", "toLong", "toFloat", "toDouble",
            "equals", "compareTo", "plus", "minus", "times", "div", "rem", "and", "or", "xor", "shl", "shr", "ushr",
            "less", "lessOrEqual", "greater", "greaterOrEqual"
        )

        private val allowedMethodsOnStrings = setOf(
            "<get-length>", "plus", "get", "compareTo", "equals", "toString"
        )

        private val allowedBuiltinExtensionFunctions = listOf(
            BuiltInOperatorNames.LESS, BuiltInOperatorNames.LESS_OR_EQUAL,
            BuiltInOperatorNames.GREATER, BuiltInOperatorNames.GREATER_OR_EQUAL,
            BuiltInOperatorNames.EQEQ, BuiltInOperatorNames.IEEE754_EQUALS,
            BuiltInOperatorNames.ANDAND, BuiltInOperatorNames.OROR
        ).map { IrBuiltIns.KOTLIN_INTERNAL_IR_FQN.child(Name.identifier(it)).asString() }.toSet()
    }
}