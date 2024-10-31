package com.github.enotvtapke.selfwritten

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.isUnsignedType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class MyIrInterpreterPrefixedFunChecker(
    private val prefix: String,
) : IrElementVisitor<Boolean, Unit> {
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

    private fun visitStatements(statements: List<IrStatement>, data: Unit): Boolean {
        return statements.all { it.accept(this, data) }
    }

    private fun visitBodyIfNeeded(irFunction: IrFunction, data: Unit): Boolean {
        return irFunction.asVisited { irFunction.body?.accept(this@MyIrInterpreterPrefixedFunChecker, data) ?: true }
    }

    private fun visitValueArguments(expression: IrFunctionAccessExpression, data: Unit): Boolean {
        return expression.args().none { it?.accept(this, data) == false }
    }

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
        return loop.asVisited {
            loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Unit): Boolean {
        return loop.asVisited {
            loop.condition.accept(this, data) && (loop.body?.accept(this, data) ?: true)
        }
    }

    override fun visitBreak(jump: IrBreak, data: Unit): Boolean = callStack.contains(jump.loop)

    override fun visitContinue(jump: IrContinue, data: Unit): Boolean = callStack.contains(jump.loop)

    override fun visitCall(expression: IrCall, data: Unit): Boolean {
        if (!expression.symbol.owner.name.asString().startsWith(prefix) && !canEvaluateFunction(expression.symbol.owner)) return false
//        if (!expression.symbol.owner.name.asString().startsWith(prefix)) return false // TODO add arg type and return type checks?
        if (expression.symbol.owner in callStack) return true

        return visitValueArguments(expression, data) && visitBodyIfNeeded(expression.symbol.owner, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: Unit): Boolean {
        return expression.symbol.owner.parent in callStack
    }

    override fun visitBody(body: IrBody, data: Unit): Boolean {
        return visitStatements(body.statements, data)
    }

    override fun visitReturn(expression: IrReturn, data: Unit): Boolean {
        if (!callStack.contains(expression.returnTargetSymbol.owner)) return false
        return expression.value.accept(this, data)
    }

    override fun visitElement(
        element: IrElement,
        data: Unit
    ): Boolean = false
}