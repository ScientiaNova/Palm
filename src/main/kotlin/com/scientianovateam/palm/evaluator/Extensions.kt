package com.scientianovateam.palm.evaluator

import com.scientianovateam.palm.registry.TypeRegistry

infix fun Any?.instanceOf(clazz: Class<*>) = this == null || clazz.isInstance(this)

infix fun Any?.cast(newType: Class<*>) =
    if (instanceOf(newType)) this
    else palmType.autoCasters[newType]?.invoke(this) ?: newType.cast(this)

val Class<out Any>.palm get() = TypeRegistry.getOrRegister(this)

val Any?.palmType get() = TypeRegistry.getOrRegister(this?.javaClass ?: Any::class.java)