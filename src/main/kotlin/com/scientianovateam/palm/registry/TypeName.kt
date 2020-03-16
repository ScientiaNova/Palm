package com.scientianovateam.palm.registry

import com.scientianovateam.palm.evaluator.palm

data class TypeName(val path: String, val name: String) {
    init {
        assert(path.all { it.isJavaIdentifierPart() || it == '.' })
        assert(name.all { it.isJavaIdentifierPart() })
    }

    fun toType() = TypeRegistry.classFromName(name, path)?.palm ?: error("Unknown type: $this")

    override fun toString() = "$path.$name"
}

fun String.toTypeName(): TypeName {
    val name = takeLastWhile { it != '.' }
    return TypeName(TypeRegistry.replaceJavaPath(dropLast(name.length + 1)), name)
}