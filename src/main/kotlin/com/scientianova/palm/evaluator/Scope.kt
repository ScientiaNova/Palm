package com.scientianova.palm.evaluator

import com.scientianova.palm.registry.*

data class Scope(
    private val values: MutableMap<String, Any?> = mutableMapOf(),
    private val parent: Scope? = GLOBAL,
    private val imports: Map<String, RegularPathNode> = emptyMap()
) {
    operator fun get(name: String): Any? = values[name] ?: parent?.get(name)
    operator fun set(name: String, obj: Any?) {
        values[name] = obj
    }

    fun getImport(name: String): RegularPathNode? = imports[name] ?: parent?.getImport(name)

    fun getType(path: List<String>): IPalmType {
        val iterator = path.iterator()
        val first = iterator.next()
        return (getImport(first) ?: RootPathNode[first])?.getType(iterator) ?: error("Unknown type")
    }

    operator fun contains(name: String): Boolean = name in values || parent?.contains(name) ?: false

    companion object {
        init {
            TypeRegistry
        }

        @JvmField
        val GLOBAL = Scope(parent = null, imports = RootPathNode["base"]!!.children)
    }
}