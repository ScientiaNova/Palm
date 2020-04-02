package com.scientianova.palm.registry

import com.scientianova.palm.evaluator.Scope
import com.scientianova.palm.evaluator.instanceOf
import com.scientianova.palm.evaluator.palm
import com.scientianova.palm.evaluator.palmType
import com.scientianova.palm.parser.IExpression
import com.scientianova.palm.util.HashMultiMap
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Modifier

interface IPalmType {
    fun createInstance(obj: Map<String, IExpression>, scope: Scope): Any
    val clazz: Class<*>
    val name: List<String>
    fun getSetter(name: String, obj: Any?, value: Any): MethodHandle?
    fun getVirtual(name: String, obj: Any?, rest: List<Any?> = emptyList()): MethodHandle?
    fun getAllStatic(): Map<String, List<StaticFunction>>
    fun getStatic(name: String, args: List<Any?> = emptyList()): StaticFunction?
    fun getVirtualCaster(obj: Any?, type: Class<*>): MethodHandle?
    fun callVirtual(name: String, scope: Scope, obj: Any?, args: List<Any?>): Any?
    fun set(name: String, obj: Any?, value: Any?)
}

operator fun IPalmType.invoke(builder: PalmType.() -> Unit) {
    if (this is PalmType) this.builder()
}

open class PalmType(override val name: List<String>, override val clazz: Class<*>) : IPalmType {
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
        for ((key, expr) in obj) if (key !in used) set(key, instance, expr.evaluate(scope))
        instance
    } ?: error("Couldn't construct object")

    override fun toString() = name.joinToString("::") { it }

    override fun getSetter(name: String, obj: Any?, value: Any) =
        virtualSetters[name] ?: clazz.superclass?.palm?.getSetter(name, obj, value)

    override fun getVirtual(name: String, obj: Any?, rest: List<Any?>) = virtual[name]?.firstOrNull {
        val type = it.type()
        type.parameterCount() == rest.size + 1 && rest.indices.all { i ->
            rest[i].instanceOf(type.parameterType(i + 1))
        }
    } ?: clazz.superclass?.palm?.getVirtual(name, obj, rest)

    override fun getAllStatic() = static

    override fun getStatic(name: String, args: List<Any?>) = static[name]?.firstOrNull {
        val type = it.handle.type()
        type.parameterCount() == args.size && args.indices.all { i ->
            args[i].instanceOf(type.parameterType(i))
        }
    }

    override fun getVirtualCaster(obj: Any?, type: Class<*>) =
        virtualCasters[type] ?: clazz.superclass?.palm?.getVirtualCaster(obj, type)

    override fun callVirtual(name: String, scope: Scope, obj: Any?, args: List<Any?>): Any? =
        scope.getMethod(name, obj, args)?.invokeWithArguments(obj, *args.toTypedArray())
            ?: error("Couldn't find a function with the signature ${obj.palmType}.$name(${args.joinToString { it.palmType.toString() }})")

    override fun set(name: String, obj: Any?, value: Any?) {
        virtualSetters[name]?.invokeWithArguments(obj, value) ?: clazz.superclass?.palm?.set(name, obj, value)
    }

    fun populate() {
        val loopUp = MethodHandles.publicLookup()
        for (constructor in clazz.declaredConstructors) {
            if (!Modifier.isPublic(constructor.modifiers)) continue
            val handle = loopUp.findConstructor(
                clazz, MethodType.methodType(Void::class.javaPrimitiveType!!, constructor.parameterTypes)
            )
            static["new"] = StaticFunction(handle)
            val annotation = constructor.getAnnotation(Palm.Constructor::class.java) ?: continue
            if (annotation.params.size != constructor.parameterCount) continue
            this.constructor = PalmConstructor(handle, annotation.params.map(PalmParameter.Companion::fromString))
            break
        }

        if (constructor == null) try {
            constructor =
                PalmConstructor(loopUp.findConstructor(clazz, MethodType.methodType(Void::class.javaPrimitiveType!!)))
        } catch (e: Exception) {
        }

        clazz.declaredMethods.forEach {
            if (!Modifier.isPublic(it.modifiers) || it.isAnnotationPresent(Palm.Ignore::class.java)) return@forEach
            val actualName = it.name
            val registryName = it.getAnnotation(Palm.Name::class.java)?.name ?: when {
                actualName.startsWith("add") || actualName.startsWith("put") || actualName.startsWith("remove") -> return@forEach
                actualName == "<init>" -> "new"
                actualName == "mod" -> "rem"
                actualName.startsWith("get") && actualName.length > 3 -> actualName.drop(3).toSnakeCase()
                else -> actualName.toSnakeCase()
            }
            if (Modifier.isStatic(it.modifiers)) {
                if (it.returnType == Void::class.javaPrimitiveType) {
                    if (registryName.startsWith("set_") && it.parameterCount == 2)
                        staticSetters[registryName.drop(4)] = loopUp.findStatic(
                            clazz, it.name, MethodType.methodType(Void::class.javaPrimitiveType, it.parameterTypes)
                        )
                    return@forEach
                }
                val handle = loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))

                static[registryName] = StaticFunction(
                    handle,
                    registryName.startsWith("to_") && it.parameterCount == 1 && it.returnType != it.parameterTypes[0]
                )
            } else {
                if (it.returnType == Void::class.javaPrimitiveType) {
                    if (registryName.startsWith("set_") && it.parameterCount == 1)
                        virtualSetters[registryName.drop(4)] = loopUp.findVirtual(
                            clazz, it.name, MethodType.methodType(Void::class.javaPrimitiveType, it.parameterTypes)
                        )
                    return@forEach
                }
                val handle = loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))

                if (registryName.startsWith("to_") && it.returnType != clazz && it.parameterCount == 0)
                    virtualCasters[it.returnType] = handle

                virtual[registryName] = handle
            }
        }

        clazz.declaredFields.forEach {
            if (!Modifier.isPublic(it.modifiers) || it.isAnnotationPresent(Palm.Ignore::class.java)) return@forEach
            val registryName = it.getAnnotation(Palm.Name::class.java)?.name
                ?: if (it.name.first().isUpperCase() && Modifier.isFinal(it.modifiers)) it.name
                else it.name.toSnakeCase()

            if (Modifier.isStatic(it.modifiers))
                static[registryName] = StaticFunction(loopUp.findStaticGetter(clazz, it.name, it.type))
            else {
                virtual[registryName] = loopUp.findGetter(clazz, it.name, it.type)
                if (!Modifier.isFinal(it.modifiers))
                    virtualSetters[registryName] = loopUp.findSetter(clazz, it.name, it.type)
            }
        }
    }

    protected val virtualSetters = hashMapOf<String, MethodHandle>()
    protected val staticSetters = hashMapOf<String, MethodHandle>()
    protected val virtual = HashMultiMap<String, MethodHandle>()
    protected val static = HashMultiMap<String, StaticFunction>()
    protected val virtualCasters = hashMapOf<Class<*>, MethodHandle>()
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

data class StaticFunction(val handle: MethodHandle, val isAutoCaster: Boolean = false)