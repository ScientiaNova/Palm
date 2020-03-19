package com.scientianova.palm.registry

import com.scientianova.palm.evaluator.Scope
import com.scientianova.palm.evaluator.instanceOf
import com.scientianova.palm.evaluator.palm
import com.scientianova.palm.evaluator.palmType
import com.scientianova.palm.parser.*
import com.scientianova.palm.util.HashMultiMap
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Modifier

val BUILT_IN = setOf("toString", "hashCode", "equals", "getClass")

interface IPalmType {
    fun createInstance(obj: Map<String, IExpression>, scope: Scope): Any
    fun get(obj: Any?, name: String): Any?
    fun set(property: String, obj: Any?, expr: IExpression, scope: Scope)
    val clazz: Class<*>
    fun iterator(obj: Any?): Iterator<*>
    val name: TypeName
    fun execute(op: UnaryOperation, obj: Any?): Any?
    fun execute(op: BinaryOperation, obj: Any?, second: Any?): Any?
    fun execute(op: MultiOperation, obj: Any?, rest: List<Any?>): Any?
    fun cast(obj: Any?, type: Class<*>): Any?
}

operator fun IPalmType.invoke(builder: PalmType.() -> Unit) {
    if (this is PalmType) this.builder()
}

open class PalmType(override val name: TypeName, override val clazz: Class<*>) : IPalmType {
    var constructor: PalmConstructor? = null

    override fun createInstance(obj: Map<String, IExpression>, scope: Scope) = constructor?.let {
        val used = mutableSetOf<String>()
        val constructorScope = Scope(parent = scope)
        val params = it.params.zip(it.handle.type().parameterList()).map { (param, type) ->
            val value = obj[param.name] ?: param.default ?: error("Missing parameter: ${param.name}")
            used += param.name
            value.handleForType(type, constructorScope).also { result -> constructorScope[param.name] = result }
        }
        val instance = it.handle.invokeWithArguments(params)
        obj.forEach { (name, expr) ->
            if (name in used) return@forEach
            set(name, instance, expr, constructorScope)
            used += name
        }
        instance
    } ?: error("Couldn't construct object")

    override fun toString() = name.toString()

    override fun get(obj: Any?, name: String) = when {
        name in getters -> getters[name]?.invokeWithArguments(obj)
        clazz.superclass != null -> clazz.superclass.palm.get(obj, name)
        else -> error("Couldn't find a property called $name for ${obj.palmType}")
    }

    override fun set(property: String, obj: Any?, expr: IExpression, scope: Scope) {
        setters[property]?.let { setter ->
            setter.invokeWithArguments(obj, expr.handleForType(setter.type().returnType(), scope))
        } ?: clazz.superclass?.palm?.set(property, obj, expr, scope)
    }

    override fun execute(op: UnaryOperation, obj: Any?) =
        unaryOps[op]?.invokeWithArguments(obj) ?: clazz.superclass?.palm?.execute(op, obj)
        ?: error("Couldn't find implementation of $op for ${obj.palmType}")

    override fun execute(op: BinaryOperation, obj: Any?, second: Any?) =
        binaryOps[op]?.firstOrNull { second instanceOf it.type().parameterType(1) }?.invokeWithArguments(obj, second)
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
        }?.invokeWithArguments(obj, *rest.toTypedArray())
            ?: clazz.superclass?.palm?.execute(op, obj, rest)
            ?: error("Couldn't find an implementation of $op for ${obj.palmType} and ${rest.joinToString { it.palmType.toString() }}")

    override fun iterator(obj: Any?) = (iterator?.invokeWithArguments(obj) ?: clazz.superclass?.palm?.iterator(obj)
    ?: error("Tried to get unimplemented iterator for ${obj.palmType}")) as Iterator<*>

    override fun cast(obj: Any?, type: Class<*>) = autoCasters[type]?.invokeWithArguments(obj)
        ?: clazz.superclass?.palm?.cast(obj, type)
        ?: type.cast(obj)

    fun populate() {
        val loopUp = MethodHandles.publicLookup()
        for (constructor in clazz.declaredConstructors) {
            val annotation = constructor.getAnnotation(Palm.Constructor::class.java) ?: continue
            if (annotation.params.size != constructor.parameterCount) continue
            this.constructor = PalmConstructor(
                loopUp.findConstructor(
                    clazz, MethodType.methodType(Void::class.javaPrimitiveType!!, constructor.parameterTypes)
                ), annotation.params.map(PalmParameter.Companion::fromString)
            )
            break
        }

        if (constructor == null) try {
            constructor =
                PalmConstructor(loopUp.findConstructor(clazz, MethodType.methodType(Void::class.javaPrimitiveType!!)))
        } catch (e: Exception) {
        }

        clazz.declaredMethods.forEach {
            if (!Modifier.isPublic(it.modifiers) || it.isAnnotationPresent(Palm.Ignore::class.java) ||
                Modifier.isStatic(it.modifiers) || it.name in BUILT_IN
            ) return@forEach
            val registryName = it.getAnnotation(Palm.Name::class.java)?.name ?: it.name
            when {
                registryName == "get" && it.parameterCount > 0 -> {
                    multiOps[Get] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                registryName == "rangeTo" && it.parameterCount in 1..2 -> {
                    multiOps[RangeTo] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                registryName.startsWith("to") && registryName.length > 2 && it.parameterCount == 0 && it.returnType != clazz && it.returnType != Void::class.javaPrimitiveType -> {
                    autoCasters[it.returnType] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType))
                }
                registryName in UNARY_OPS -> {
                    val op = UNARY_OPS[registryName]
                    if (it.parameterCount == 0)
                        unaryOps[op!!] =
                            loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType))
                }
                it.parameterCount == 0 && it.returnType != Void::class.javaPrimitiveType -> {
                    val getterName = (if (registryName.startsWith("get")) registryName.drop(3)
                        .decapitalize() else registryName).toSnakeCase()
                    val method = loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType))
                    if (registryName == "iter" || registryName == "iterator") iterator = method
                    getters[getterName] = method
                }
                it.parameterCount == 1 && it.returnType == Void::class.javaPrimitiveType -> {
                    val setterName = (if (registryName.startsWith("set")) registryName.drop(3)
                        .decapitalize() else registryName).toSnakeCase()
                    setters[setterName] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                else -> BINARY_OPS[registryName]?.let { op ->
                    if (it.parameterCount == 1 && op.returnType?.equals(it.returnType) != false)
                        binaryOps[op] =
                            loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
            }
        }

        clazz.declaredFields.forEach {
            if (!Modifier.isPublic(it.modifiers) || it.isAnnotationPresent(Palm.Ignore::class.java) ||
                Modifier.isStatic(it.modifiers)
            ) return@forEach
            val registryName = it.getAnnotation(Palm.Name::class.java)?.name ?: it.name
            getters[registryName] = loopUp.findGetter(clazz, it.name, it.type)
            if (!Modifier.isFinal(it.modifiers))
                setters[registryName] = loopUp.findSetter(clazz, it.name, it.type)
        }
    }

    var iterator: MethodHandle? = null

    val getters = hashMapOf<String, MethodHandle>()
    val setters = hashMapOf<String, MethodHandle>()

    val unaryOps = hashMapOf<UnaryOperation, MethodHandle>()
    val binaryOps = HashMultiMap<BinaryOperation, MethodHandle>()
    val multiOps = HashMultiMap<MultiOperation, MethodHandle>()
    val autoCasters = hashMapOf<Class<*>, MethodHandle>()
}

data class PalmConstructor(val handle: MethodHandle, val params: List<PalmParameter> = emptyList())

data class PalmParameter(val name: String, val default: IExpression? = null) {
    companion object {
        fun fromString(string: String): PalmParameter {
            val parts = string.split('=', limit = 2)
            val name = parts.first().dropLastWhile { it.isWhitespace() }
            return PalmParameter(name, parts.lastOrNull()?.parseExpression("default for $name"))
        }
    }
}