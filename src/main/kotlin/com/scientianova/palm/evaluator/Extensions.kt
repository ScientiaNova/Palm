package com.scientianova.palm.evaluator

import com.scientianova.palm.registry.TypeRegistry
import kotlin.reflect.KClass

infix fun Any?.instanceOf(clazz: Class<*>) = this == null || clazz.isInstance(this)

infix fun Any?.cast(type: Class<*>) = when {
    instanceOf(type) -> this
    type == String::class.java -> toString()
    else -> palmType.cast(this, type)
}

val Class<out Any>.palm get() = TypeRegistry.getOrRegister(this)
val KClass<out Any>.palm get() = java.palm

val Any?.palmType get() = TypeRegistry.getOrRegister(this?.javaClass ?: Any::class.java)