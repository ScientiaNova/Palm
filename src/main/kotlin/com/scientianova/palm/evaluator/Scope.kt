package com.scientianova.palm.evaluator

import com.scientianova.palm.parser.Imports
import com.scientianova.palm.registry.*
import com.scientianova.palm.util.HashMultiMap
import com.scientianova.palm.util.None
import com.scientianova.palm.util.Optional
import com.scientianova.palm.util.Some
import java.lang.invoke.MethodHandle

data class Scope(
    private val values: MutableMap<String, Any?> = mutableMapOf(),
    private val parent: Scope? = GLOBAL,
    internal val imports: MutableMap<String, RegularPathNode> = mutableMapOf(),
    internal val staticImports: HashMultiMap<String, MethodHandle> = HashMultiMap(),
    private val staticCasters: HashMultiMap<Class<*>, MethodHandle> = HashMultiMap()
) {
    constructor(values: MutableMap<String, Any?> = mutableMapOf(), parent: Scope? = GLOBAL, imports: Imports) :
            this(values, parent, imports.paths, imports.static, imports.casters)

    fun getInScope(name: String): Optional<Any?> =
        if (name in values) Some(values[name]) else parent?.getInScope(name) ?: None

    operator fun get(name: String): Any? = when (val optional = getInScope(name)) {
        is Some -> optional.value
        is None -> staticImports[name]?.firstOrNull { it.type().parameterCount() == 0 }?.invokeWithArguments()
            ?: error("Not such variable called $name in scope")
    }

    operator fun set(name: String, obj: Any?) {
        values[name] = obj
    }

    fun addImports(otherImports: Imports) {
        imports += otherImports.paths
        staticImports += otherImports.static
        staticCasters += otherImports.casters
    }

    fun import(type: IPalmType) {
        val simpleName = type.name.last()
        imports[simpleName] = RootPathNode.getNode(type.name)
        type.constructors.forEach { staticImports[simpleName] = it }
    }

    fun importAll(type: IPalmType) {
        val node = RootPathNode.getNode(type.name)
        addImports(node.getAllImports())
    }

    fun getImport(name: String): RegularPathNode? = imports[name] ?: parent?.getImport(name)

    fun getNode(path: List<String>): RegularPathNode {
        val iterator = path.iterator()
        val first = iterator.next()
        return getImport(first) ?: RootPathNode[first] ?: error("Unknown path node")
    }

    fun getType(path: List<String>) = getNode(path).type ?: error("Unknown type")

    fun addStaticImport(name: String, function: StaticFunction) {
        val handle = function.handle
        staticImports[name] = handle
        if (function.isAutoCaster)
            staticCasters[handle.type().returnType()] = handle
    }

    fun getStaticCasters(type: Class<*>): List<MethodHandle> {
        val parentCasters = parent?.getStaticCasters(type) ?: emptyList()
        return staticCasters[type]?.plus(parentCasters) ?: parentCasters
    }

    fun getStaticImports(name: String): List<MethodHandle> {
        val parentImports = parent?.getStaticImports(name) ?: emptyList()
        return staticImports[name]?.plus(parentImports) ?: parentImports
    }

    fun getMethod(name: String, obj: Any?, args: List<Any?>) =
        obj.palmType.getVirtual(name, obj, args) ?: getStaticImports(name).firstOrNull {
            val type = it.type()
            obj instanceOf type.parameterType(0) && type.parameterCount() == args.size + 1 && args.indices.all { i ->
                args[i] instanceOf type.parameterType(i + 1)
            }
        }

    fun callVirtual(name: String, obj: Any?, args: List<Any?>): Any? = handlePrimitives(name, obj, args)
        ?: getMethod(name, obj, args)?.invokeWithArguments(obj, *args.toTypedArray())
        ?: error("Couldn't find a function with the signature ${obj.palmType}.$name(${args.joinToString { it.palmType.toString() }})")

    fun callVirtual(name: String, obj: Any?, vararg args: Any?): Any? = handlePrimitives(name, obj, args.toList())
        ?: getMethod(name, obj, args.toList())?.invokeWithArguments(obj, *args)
        ?: error("Couldn't find a function with the signature ${obj.palmType}.$name(${args.joinToString { it.palmType.toString() }})")

    fun getIterator(obj: Any?) = callVirtual("iterator", obj) as Iterator<*>

    fun getCaster(obj: Any?, type: Class<*>) =
        obj.palmType.getVirtualCaster(obj, type) ?: getStaticCasters(type).firstOrNull {
            obj instanceOf it.type().parameterType(0)
        }

    fun cast(obj: Any?, type: Class<*>) = when {
        obj instanceOf type -> obj
        type == String::class.java -> obj.toString()
        else -> getCaster(obj, type)?.invokeWithArguments(obj) ?: error("Couldn't cast ${obj.palmType} to ${type.palm}")
    }

    fun getStatic(name: String, path: List<String>, args: List<Any?>): MethodHandle? =
        (if (path.isEmpty()) getStaticImports(name) else getNode(path).getStatic(name)).firstOrNull {
            val type = it.type()
            args.size == type.parameterCount() && args.indices.all { i -> args[i] instanceOf type.parameterType(i) }
        }

    fun callStatic(name: String, path: List<String>, args: List<Any?>) =
        getStatic(name, path, args)?.invokeWithArguments(args)
            ?: error("Couldn't find a function with the signature $name(${args.joinToString { it.palmType.toString() }})")

    operator fun contains(name: String): Boolean = name in values || parent?.contains(name) ?: false

    companion object {
        @JvmField
        val GLOBAL = Scope(parent = null)

        init {
            TypeRegistry
        }
    }
}