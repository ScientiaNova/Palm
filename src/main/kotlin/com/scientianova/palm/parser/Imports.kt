package com.scientianova.palm.parser

import com.scientianova.palm.registry.RegularPathNode
import com.scientianova.palm.util.HashMultiMap
import java.lang.invoke.MethodHandle

data class Imports(
    val paths: MutableMap<String, RegularPathNode> = mutableMapOf(),
    val static: HashMultiMap<String, MethodHandle> = HashMultiMap(),
    val casters: HashMultiMap<Class<*>, MethodHandle> = HashMultiMap()
) {
    operator fun plusAssign(other: Imports) {
        paths.putAll(other.paths)
        static.putAll(other.static)
        casters.putAll(other.casters)
    }
}