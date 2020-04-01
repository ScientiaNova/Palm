package com.scientianova.palm.evaluator

import com.scientianova.palm.registry.TypeRegistry
import kotlin.reflect.KClass

infix fun Any?.instanceOf(clazz: Class<*>) = this == null || clazz.isInstance(this) || this::class.javaPrimitiveType == clazz

val Class<out Any>.palm get() = TypeRegistry.getOrRegister(this)
val KClass<out Any>.palm get() = java.palm

val Any?.palmType get() = TypeRegistry.getOrRegister(this?.javaClass ?: Any::class.java)

fun Any?.callVirtual(name: String, scope: Scope, args: List<Any?>) = palmType.callVirtual(name, scope, this, args)
fun Any?.callVirtual(name: String, scope: Scope, vararg args: Any?) = palmType.callVirtual(name, scope, this, args.toList())