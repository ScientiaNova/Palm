package com.scientianovateam.palm.registry

import com.scientianovateam.palm.evaluator.Scope
import com.scientianovateam.palm.evaluator.instanceOf
import com.scientianovateam.palm.evaluator.palm
import com.scientianovateam.palm.evaluator.palmType
import com.scientianovateam.palm.parser.BinaryOperation
import com.scientianovateam.palm.parser.IExpression
import com.scientianovateam.palm.parser.MultiOperation
import com.scientianovateam.palm.parser.UnaryOperation
import com.scientianovateam.palm.util.HashMultiMap
import java.lang.invoke.MethodHandle

interface IPalmType {
    fun createInstance(obj: Map<String, IExpression>, scope: Scope): Any
    fun get(obj: Any?, name: String): Any?
    val clazz: Class<*>
    val iterator: MethodHandle?
    val name: String
    fun execute(op: UnaryOperation, obj: Any?): Any?
    fun execute(op: BinaryOperation, obj: Any?, second: Any?): Any?
    fun execute(op: MultiOperation, obj: Any?, rest: List<Any?>): Any?
    val autoCasters: Map<Class<*>, MethodHandle>
}

data class PalmType(
    override val name: String,
    override val clazz: Class<*>,
    val constructor: PalmConstructor? = null
) : IPalmType {
    override fun createInstance(obj: Map<String, IExpression>, scope: Scope) = constructor?.let {
        val used = mutableSetOf<String>()
        val params = it.args.map { (name, type) ->
            val value = obj[name] ?: error("Missing parameter: $name")
            used += name
            value.handleForType(type, scope)
        }
        val instance = constructor.handle.invokeWithArguments(params)
        obj.forEach { (name, expr) ->
            if (name in used) return@forEach
            setters[name]?.let { setter ->
                it.handle.invoke(instance, expr.handleForType(setter.type().returnType(), scope))
                used += name
            }
        }
        instance
    } ?: error("Couldn't construct object")

    override fun toString() = name

    override fun get(obj: Any?, name: String) = getters[name]?.invoke(obj) ?: clazz.superclass?.palm?.get(obj, name)

    override fun execute(op: UnaryOperation, obj: Any?) =
        unaryOps[op]?.invoke(obj) ?: clazz.superclass?.palm?.execute(op, obj)
        ?: error("Couldn't find implementation of $op for ${obj.palmType}")

    override fun execute(op: BinaryOperation, obj: Any?, second: Any?) =
        binaryOps[op]?.firstOrNull { second instanceOf it.type().parameterType(1) }?.invoke(obj, second)
            ?: clazz.superclass?.palm?.execute(op, obj, second)
            ?: error("Couldn't find implementation of $op for ${obj.palmType} and ${second.palmType}")

    override fun execute(op: MultiOperation, obj: Any?, rest: List<Any?>) =
        multiOps[op]?.firstOrNull {
            it.type().parameterCount() == 1 + rest.size && run {
                for (i in 1 until rest.size)
                    if (!(rest[i] instanceOf it.type().parameterArray()[i]))
                        return@run false
                true
            }
        }?.invoke(obj, rest.toTypedArray())
            ?: clazz.superclass?.palm?.execute(op, obj, rest)
            ?: error("Couldn't find an implementation of $op for ${obj.palmType} and ${rest.joinToString { it.palmType.toString() }}")

    override var iterator: MethodHandle? = null

    val getters = hashMapOf<String, MethodHandle>()
    val setters = hashMapOf<String, MethodHandle>()

    val unaryOps = hashMapOf<UnaryOperation, MethodHandle>()
    val binaryOps = HashMultiMap<BinaryOperation, MethodHandle>()
    val multiOps = HashMultiMap<MultiOperation, MethodHandle>()
    override val autoCasters = hashMapOf<Class<*>, MethodHandle>()
}

data class PalmConstructor(val handle: MethodHandle, val args: Map<String, Class<*>>)