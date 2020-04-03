package com.scientianova.palm.registry

import com.scientianova.palm.parser.Imports
import com.scientianova.palm.util.HashMultiMap
import java.lang.invoke.MethodHandle

sealed class PathNode {
    internal val children = mutableMapOf<String, RegularPathNode>()
    operator fun get(next: String) = children[next]
    operator fun set(name: String, node: RegularPathNode) {
        children[name] = node
    }

    fun getImports(name: String, alias: String = name): Imports {
        val paths = mutableMapOf<String, RegularPathNode>()
        val static = HashMultiMap<String, MethodHandle>()
        val casters = HashMultiMap<Class<*>, MethodHandle>()

        children[name]?.let { child ->
            paths[alias] = child
            child.type?.constructors?.forEach { static[alias] = it }
        }

        if (this is RegularPathNode)
            type?.static?.get(alias)?.forEach {
                static[alias] = it.handle
                if (it.isAutoCaster) casters[it.handle.type().returnType()] = it.handle
            }

        return Imports(paths, static, casters)
    }

    fun getAllImports(): Imports {
        val paths = mutableMapOf<String, RegularPathNode>()
        val static = HashMultiMap<String, MethodHandle>()
        val casters = HashMultiMap<Class<*>, MethodHandle>()

        children.forEach { (name, child) ->
            paths[name] = child
            child.type?.constructors?.forEach { static[name] = it }
        }

        if (this is RegularPathNode)
            type?.static?.forEach { (name, functions) ->
                functions.forEach {
                    static[name] = it.handle
                    if (it.isAutoCaster) casters[it.handle.type().returnType()] = it.handle
                }
            }

        return Imports(paths, static, casters)
    }
}

fun List<String>.toType() = RootPathNode.getType(this)

object RootPathNode : PathNode() {
    fun getNode(path: List<String>): RegularPathNode {
        val iterator = path.iterator()
        return (get(iterator.next()) ?: error("Invalid type path")).getNote(iterator)
    }

    fun getType(path: List<String>) = getNode(path).type

    internal fun addType(path: List<String>, type: IPalmType): RegularPathNode {
        val iterator = path.iterator()
        val start = iterator.next()
        return addType(iterator, type, get(start) ?: RegularPathNode(this).also { set(start, it) })
    }

    private fun addType(pathIterator: Iterator<String>, type: IPalmType, lastNode: RegularPathNode): RegularPathNode =
        if (pathIterator.hasNext()) {
            val next = pathIterator.next()
            addType(pathIterator, type, lastNode[next] ?: RegularPathNode(lastNode).also { lastNode[next] = it })
        } else lastNode.also { it.type = type }
}

class RegularPathNode(val parent: PathNode) : PathNode() {
    var type: IPalmType? = null
        internal set

    fun getStatic(name: String): List<MethodHandle> {
        val list = mutableListOf<MethodHandle>()
        children[name]?.let { it.type?.constructors?.forEach { constructor -> list += constructor } }
        type?.let { it.static[name]?.forEach { func -> list += func.handle } }
        return list
    }

    fun getNote(pathIterator: Iterator<String>): RegularPathNode =
        if (pathIterator.hasNext())
            (this[pathIterator.next()] ?: error("Invalid type path")).getNote(pathIterator)
        else this

    fun getType(pathIterator: Iterator<String>) = getNote(pathIterator).type
}