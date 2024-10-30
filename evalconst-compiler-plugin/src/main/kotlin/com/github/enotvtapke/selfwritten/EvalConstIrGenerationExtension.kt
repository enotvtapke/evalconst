package com.github.enotvtapke.selfwritten

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

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

        // TODO determine that call wasn't done from another eval function
        if (callee.name.asString().startsWith(prefix)) {
            val res = expression.accept(BodyEvaluator(prefix, limit), Unit)
            return res
        }

        return expression
    }
}

sealed interface Constant {
    class StringConst(val v: String) : Constant
    class IntConst(val v: Int) : Constant
    class BoolConst(val v: Boolean) : Constant
}

class Scopes {
    private val scopes: ArrayDeque<MutableMap<Name, IrConst<*>>> = ArrayDeque(listOf(mutableMapOf()))

    fun enterScope() {
        scopes.addLast(mutableMapOf())
    }

    fun leaveScope() {
        scopes.removeLast()
    }

    fun defineVar(name: Name, value: IrConst<*>) {
        val scope = scopes.last()
        if (scope.containsKey(name)) {
            throw IllegalStateException("Variable `$name` is already defined in scope")
        }
        scope[name] = value
    }

    fun getVar(name: Name): IrConst<*> = scopes.findLast { it.containsKey(name) }?.let { it[name] }
        ?: throw IllegalStateException("Variable `$name` is not defined")
}

private data class State(val s: Map<String, IrConst<*>>)

private class BodyEvaluator(private val prefix: String, private val limit: Int) : IrElementVisitor<IrConst<*>, Unit> {
    private var scopes = Scopes()

    override fun visitCall(callSite: IrCall, state: Unit): IrConst<*> {
        val callee = callSite.symbol.owner
        val calleeName = callee.name.toString()

        if (calleeName.startsWith(prefix)) {
            val newScopes = Scopes()
            val valueArguments = callee.valueParameters.map { callSite.getValueArgument(it.index) }.toMutableList()
            for (parameter in callee.valueParameters) {
                val argument = valueArguments[parameter.index]
                newScopes.defineVar(parameter.name, argument!!.accept(this, Unit))
            }
            val savedScopes = scopes
            scopes = newScopes
            return eval(callee).also {
                scopes = savedScopes
            }
        }

        return TODO()
    }

    override fun visitReturn(
        expression: IrReturn, data: Unit
    ): IrConst<*> {
        return expression.value.accept(this, Unit)
    }

    override fun visitVariable(
        declaration: IrVariable, data: Unit
    ): IrConst<*> {
        scopes.defineVar(declaration.name, declaration.initializer!!.accept(this, Unit))
        return scopes.getVar(declaration.name) // TODO Shouldn't be used
    }

    override fun visitGetValue(
        expression: IrGetValue, data: Unit
    ): IrConst<*> {
        return scopes.getVar(expression.symbol.owner.name)
    }

    override fun visitConst(
        expression: IrConst<*>, data: Unit
    ): IrConst<*> {
        return expression
    }

    private fun eval(f: IrSimpleFunction): IrConst<*> {
        return f.body!!.statements.map { it.accept(this, Unit) }.last()
    }

    override fun visitElement(
        element: IrElement, data: Unit
    ): IrConst<*> {
        TODO("Not yet implemented")
    }
}
