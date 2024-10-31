package com.github.enotvtapke.evalconst.evaluator

import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.name.Name

class EvalEnvironment {
    class Scope {
        private val scopes: ArrayDeque<MutableMap<Name, IrConst<*>>> = ArrayDeque(listOf(mutableMapOf()))

        fun enterScope() = scopes.addLast(mutableMapOf())
        fun leaveScope() = scopes.removeLast()

        fun defineVar(name: Name, value: IrConst<*>) {
            val scope = scopes.last()
            if (scope.containsKey(name)) throw IllegalStateException("Variable `$name` is already defined in scope")
            scope[name] = value
        }

        fun getVar(name: Name): IrConst<*> = scopes.findLast { it.containsKey(name) }?.let { it[name] }
            ?: throw IllegalStateException("Variable `$name` is not defined")

        fun setVar(name: Name, value: IrConst<*>) {
            scopes.findLast { it.containsKey(name) }?.also { it[name] = value }
                ?: throw IllegalStateException("Variable `$name` is not defined")
        }
    }

    private val callStack: ArrayDeque<Scope> = ArrayDeque(listOf(Scope()))

    fun stackSize() = callStack.size

    fun defineVar(name: Name, value: IrConst<*>) = callStack.last().defineVar(name, value)
    fun getVar(name: Name) = callStack.last().getVar(name)
    fun setVar(name: Name, value: IrConst<*>) = callStack.last().setVar(name, value)

    fun enterScope() = callStack.last().enterScope()
    fun leaveScope() = callStack.last().leaveScope()

    inline fun <T> inScope(crossinline f: () -> T): T {
        enterScope()
        return f().also {
            leaveScope()
        }
    }

    fun enterFun() = callStack.addLast(Scope())
    fun leaveFun() = callStack.removeLast()

    inline fun <T> inFun(crossinline f: () -> T): T {
        enterFun()
        return f().also {
            leaveFun()
        }
    }
}
