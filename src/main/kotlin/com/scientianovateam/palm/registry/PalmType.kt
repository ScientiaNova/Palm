package com.scientianovateam.palm.registry

import com.scientianovateam.palm.parser.BinaryOperation
import com.scientianovateam.palm.parser.MultiOperation
import com.scientianovateam.palm.parser.UnaryOperation
import com.scientianovateam.palm.util.HashMultiMap
import java.lang.invoke.MethodHandle
import kotlin.reflect.KType

data class PalmType(val constructor: PalmConstructor? = null) {
    val getters = hashMapOf<String, MethodHandle>()
    val setters = hashMapOf<String, MethodHandle>()
    val unaryOps = hashMapOf<UnaryOperation, MethodHandle>()
    val binaryOps = HashMultiMap<BinaryOperation, MethodHandle>()
    val multiOps = HashMultiMap<MultiOperation, MethodHandle>()
    val autoCaster = hashMapOf<Class<*>, MethodHandle>()
}

data class PalmConstructor(val handle: MethodHandle, val args: Map<String, Class<*>>)