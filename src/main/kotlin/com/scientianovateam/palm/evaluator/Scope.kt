package com.scientianovateam.palm.evaluator

data class Scope(private val values: MutableMap<String, Any?> = mutableMapOf(), private val parent: Scope? = null) {
    operator fun get(name: String): Any? = values[name] ?: parent?.get(name)
    operator fun set(name: String, obj: Any?) {
        values[name] = obj
    }
}