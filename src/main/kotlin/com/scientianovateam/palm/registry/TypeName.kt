package com.scientianovateam.palm.registry

data class TypeName(val path: String, val name: String) {
    init {
        assert(path.all { it.isJavaIdentifierPart() || it == '.' })
        assert(name.all { it.isJavaIdentifierPart() })
    }

    override fun toString() = "$path.$name"
}

fun String.toTypeName(): TypeName {
    val name = takeLastWhile { it != '.' }
    return TypeName(TypeRegistry.replaceJavaPath(dropLast(name.length + 1)), name)
}