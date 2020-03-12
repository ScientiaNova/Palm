package com.scientianovateam.palm.evaluator

data class Scope(val values: MutableMap<String, PalmObject>, val parent: Scope? = null) {
    operator fun get(name: String): PalmObject = values[name] ?: parent?.get(name) ?: NULL_OBJECT
}