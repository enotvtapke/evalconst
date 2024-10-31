package com.github.enotvtapke.evalconst.evaluator

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl.Companion.constNull
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterError
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

fun IrFunctionAccessExpression.args(): List<IrExpression?> = listOfNotNull(dispatchReceiver) + valueArguments

class ConstFunEvaluator(
    private val isEvalFunction: (IrFunction) -> Boolean,
    private val stepLimit: Int,
    private val stackSizeLimit: Int,
) : IrElementVisitor<IrConst<*>, Unit> {
    private var env = EvalEnvironment()
    private var statementNumber: Int = 0

    override fun visitCall(call: IrCall, state: Unit): IrConst<*> {
        val callee = call.symbol.owner

        if (env.stackSize() > stackSizeLimit)
            throw EvaluatorStackSizeException("Stack size exceeds configured limit `$stackSizeLimit`")

        return if (isEvalFunction(callee)) {
            val argsValues = call.valueArguments.map { it!!.accept(this, Unit) }
            env.inFun {
                callee.valueParameters.forEach {
                    env.defineVar(it.name, argsValues[it.index].accept(this, Unit))
                }
                evalBody(callee.body!!)
            }
        } else {
            evalBuiltInFunction(call).toIrConst(call.type)
        }
    }

    override fun visitConst(expression: IrConst<*>, data: Unit): IrConst<*> = expression

    override fun visitGetValue(expression: IrGetValue, data: Unit): IrConst<*> =
        env.getVar(expression.symbol.owner.name)

    override fun visitSetValue(expression: IrSetValue, data: Unit): IrConst<*> =
        expression.value.accept(this, data).also {
            env.setVar(expression.symbol.owner.name, it)
        }

    override fun visitVariable(declaration: IrVariable, data: Unit): IrConst<*> =
        declaration.initializer!!.accept(this, Unit).also {
            env.defineVar(declaration.name, it)
        }

    override fun visitReturn(expression: IrReturn, data: Unit): IrConst<*> = expression.value.accept(this, Unit)

    override fun visitWhen(expression: IrWhen, data: Unit): IrConst<*> =
        env.inScope {
            expression.branches.find { it.condition.accept(this, data).value as Boolean }?.result?.accept(this, data)
                ?: NULL_CONST
        }

    override fun visitWhileLoop(loop: IrWhileLoop, data: Unit): IrConst<*> {
        return env.inScope {
            while (loop.condition.accept(this, data).value as Boolean) {
                loop.body!!.accept(this, data)
                addStepAndCheck()
            }
            NULL_CONST
        }
    }

    override fun visitBody(body: IrBody, data: Unit): IrConst<*> = evalStatements(body.statements)

    override fun visitBlock(block: IrBlock, data: Unit): IrConst<*> = evalStatements(block.statements)

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Unit): IrConst<*> =
        env.inScope {
            do {
                loop.body!!.accept(this, data)
                addStepAndCheck()
            } while (loop.condition.accept(this, data).value as Boolean)
            NULL_CONST
        }

    override fun visitElement(
        element: IrElement, data: Unit
    ): IrConst<*> {
        throw IllegalArgumentException("Evaluation of element `${element.dump()}` is not supported")
    }

    private fun evalBody(body: IrBody): IrConst<*> = evalStatements(body.statements)
    private fun evalStatements(statements: List<IrStatement>): IrConst<*> =
        statements.map {
            it.accept(this, Unit)
        }.lastOrNull() ?: NULL_CONST

    private fun addStepAndCheck() {
        statementNumber += 1
        if (statementNumber > stepLimit)
            throw EvaluatorStatementsLimitException("Number of evaluated statements exceeds configured limit `$stepLimit`")
    }

    private fun evalBuiltInFunction(call: IrCall): Any? {
        val irFunction = call.symbol.owner
        val argsValues = call.args().map { it?.accept(this, Unit)?.value }
        val argsTypes = (listOfNotNull(irFunction.dispatchReceiverParameter) + irFunction.valueParameters)
            .map { it.type.fqNameWithNullability() }
        return callBuiltinFunction(Signature(
            irFunction.name.asString(),
            argsTypes.zip(argsValues).map { Arg(it.first, it.second) }
        ))
    }

    private fun IrType.fqNameWithNullability(): String {
        val fqName = classFqName?.toString()
        val nullability =
            if (this is IrSimpleType && this.nullability == SimpleTypeNullability.MARKED_NULLABLE) "?" else ""
        return fqName + nullability
    }

    private fun callBuiltinFunction(signature: Signature): Any? {
        val name = signature.name
        val args = signature.args
        return when (args.size) {
            1 -> interpretUnaryFunction(name, args[0].type, args[0].value)
            2 -> interpretBinaryFunction(name, args[0].type, args[1].type, args[0].value, args[1].value)
            3 -> interpretTernaryFunction(
                name,
                args[0].type,
                args[1].type,
                args[2].type,
                args[0].value,
                args[1].value,
                args[2].value
            )

            else -> throw InterpreterError("Unsupported number of arguments for invocation as builtin function: $name")
        }
    }

    private data class Signature(var name: String, var args: List<Arg>)
    private data class Arg(var type: String, var value: Any?)
    companion object {
        private val NULL_CONST = constNull(0, 0, IrUninitializedType)
    }
}
