package com.scientianova.palm.evaluator

data class Scope(private val values: MutableMap<String, Any?> = mutableMapOf(), private val parent: Scope? = GLOBAL) {
    operator fun get(name: String): Any? = values[name] ?: parent?.get(name)
    operator fun set(name: String, obj: Any?) {
        values[name] = obj
    }

    operator fun contains(name: String): Boolean = name in values || parent?.contains(name) ?: false

    companion object {
        @JvmField
        val GLOBAL = Scope(parent = null)
    }
}