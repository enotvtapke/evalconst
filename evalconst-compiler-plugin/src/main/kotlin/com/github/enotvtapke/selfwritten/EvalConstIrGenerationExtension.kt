package com.github.enotvtapke.selfwritten

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.interpreter.checker.IrInterpreterCheckerData
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterError
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class EvalConstIrGenerationExtension(private val prefix: String, private val limit: Int) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(AccessorInliner(prefix, limit, moduleFragment), null)
    }
}

private class AccessorInliner(
    private val prefix: String, private val limit: Int, private val moduleFragment: IrModuleFragment
) : IrElementTransformerVoid() {


    @OptIn(ObsoleteDescriptorBasedAPI::class)
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

class Scope {
    private val scopesStack: ArrayDeque<MutableMap<Name, IrConst<*>>> = ArrayDeque(listOf(mutableMapOf()))

    fun enterScope() {
        scopesStack.addLast(mutableMapOf())
    }

    fun leaveScope() {
        scopesStack.removeLast()
    }

    fun defineVar(name: Name, value: IrConst<*>) {
        val scope = scopesStack.last()
        if (scope.containsKey(name)) {
            throw IllegalStateException("Variable `$name` is already defined in scope")
        }
        scope[name] = value
    }

    fun getVar(name: Name): IrConst<*> = scopesStack.findLast { it.containsKey(name) }?.let { it[name] }
        ?: throw IllegalStateException("Variable `$name` is not defined")
}

private data class Signature(var name: String, var args: List<Arg>)
private data class Arg(var type: String, var value: Any?)

private data class State(val s: Map<String, IrConst<*>>)

fun IrFunctionAccessExpression.args(): List<IrExpression?> = listOfNotNull(dispatchReceiver) + valueArguments

// TODO IrElementVisitorVoid?
private class BodyEvaluator(private val prefix: String, private val limit: Int) : IrElementVisitor<IrConst<*>, Unit> {
    private var scope = Scope()

    fun IrType.fqNameWithNullability(): String {
        val fqName = classFqName?.toString()
        val nullability = if (this is IrSimpleType && this.nullability == SimpleTypeNullability.MARKED_NULLABLE) "?" else ""
        return fqName + nullability
    }

    private fun calculateBuiltIn(call: IrCall): Any? {
        val irFunction = call.symbol.owner
        val receiverValue = call.dispatchReceiver?.accept(this, Unit)?.value
        val argsValues = listOfNotNull(receiverValue) + call.valueArguments.map { it?.accept(this, Unit)?.value }
        val receiverType = irFunction.dispatchReceiverParameter?.type
        val argsType = (listOfNotNull(receiverType) + irFunction.valueParameters.map { it.type }).map { it.fqNameWithNullability() }

        return callBuiltinFunction(Signature(
            irFunction.name.asString(),
            argsType.zip(argsValues).map { Arg(it.first, it.second) }
        ))
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

    override fun visitCall(call: IrCall, state: Unit): IrConst<*> {
        val callee = call.symbol.owner
        val calleeName = callee.name.toString()

        if (calleeName.startsWith(prefix)) {
            val newScope = Scope()
            val valueArguments = callee.valueParameters.map { call.getValueArgument(it.index) }.toMutableList()
            for (parameter in callee.valueParameters) {
                val argument = valueArguments[parameter.index]
                newScope.defineVar(parameter.name, argument!!.accept(this, Unit))
            }
            val savedScopes = scope
            scope = newScope
            return evalBody(callee.body!!).also {
                scope = savedScopes
            }
        } else {
            return calculateBuiltIn(call).toIrConst(call.type)
        }
    }

    override fun visitReturn(
        expression: IrReturn, data: Unit
    ): IrConst<*> {
        return expression.value.accept(this, Unit)
    }

    override fun visitVariable(
        declaration: IrVariable, data: Unit
    ): IrConst<*> {
        scope.defineVar(declaration.name, declaration.initializer!!.accept(this, Unit))
        return scope.getVar(declaration.name) // TODO Shouldn't be used
    }

    override fun visitGetValue(
        expression: IrGetValue, data: Unit
    ): IrConst<*> {
        return scope.getVar(expression.symbol.owner.name)
    }

    override fun visitConst(
        expression: IrConst<*>, data: Unit
    ): IrConst<*> = expression

    private fun evalBody(body: IrBody): IrConst<*> {
        return body.statements.map { it.accept(this, Unit) }.last()
    }

    override fun visitElement(
        element: IrElement, data: Unit
    ): IrConst<*> {
        TODO("Not yet implemented")
    }
}
