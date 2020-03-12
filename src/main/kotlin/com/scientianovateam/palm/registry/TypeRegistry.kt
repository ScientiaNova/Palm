package com.scientianovateam.palm.registry

import com.scientianovateam.palm.parser.BINARY_OPS
import com.scientianovateam.palm.parser.Get
import com.scientianovateam.palm.parser.ToRange
import com.scientianovateam.palm.parser.UNARY_OPS
import com.scientianovateam.palm.util.HashBasedTable
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Modifier
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure

object TypeRegistry {
    private val registry = HashBasedTable<String, String, Class<*>>()
    fun registerPalmName(path: String, name: String, clazz: Class<*>) {
        registry[name, path] = clazz
    }

    fun classFromName(name: String, path: String? = null) =
        if (path.isNullOrBlank()) registry[name]?.values?.firstOrNull() else registry[name, path]

    private val types = hashMapOf<Class<*>, PalmType>()

    init {
        register(Any::class.java, "Base", "Any")
        register(Byte::class.java, "Base")
        register(Int::class.java, "Base", "Int")
        register(Float::class.java, "Base")
        register(Double::class.java, "Base")
        register(Long::class.java, "Base")
        register(Boolean::class.java, "Base", "Bool")
        register(Char::class.java, "Base", "Char")
        register(String::class.java, "Base")
        register(List::class.java, "Base")
        register(ArrayList::class.java, "Base")
        register(Map::class.java, "Base")
        register(HashMap::class.java, "Base")
        register(LinkedHashMap::class.java, "Base")
        register(Pair::class.java, "Base")
        register(Nothing::class.java, "Base")
    }

    fun registerType(clazz: Class<*>) {
        val loopUp = MethodHandles.lookup()
        val constructor = clazz.kotlin.constructors.firstOrNull {
            it.visibility == KVisibility.PUBLIC && it.findAnnotation<Palm.Ignore>() == null
        }?.let { constructor ->
            val names = constructor.findAnnotation<Palm.Constructor>()?.paramNames ?: emptyArray()
            val paramMap = (if (names.size == constructor.parameters.size)
                (0..names.size).map { names[it] to constructor.parameters[it].type.jvmErasure.java }
            else constructor.parameters.map { it.name!! to it.type.jvmErasure.java }).toMap()
            PalmConstructor(
                loopUp.findConstructor(
                    clazz, MethodType.methodType(clazz, constructor.parameters.map { it.type.jvmErasure.java })
                ), paramMap
            )
        }
        val type = PalmType(constructor)
        types[clazz] = type
        clazz.methods.forEach {
            if (!Modifier.isPublic(it.modifiers) || it.isAnnotationPresent(Palm.Ignore::class.java) || Modifier.isStatic(
                    it.modifiers
                )
            ) return@forEach
            val registryName = it.getAnnotation(Palm.Name::class.java)?.name ?: it.name
            when {
                registryName == "get" && it.parameterCount > 1 -> {
                    type.multiOps[Get] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                registryName == "rangeTo" && it.parameterCount in 2..3 -> {
                    type.multiOps[ToRange] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                registryName.length > 3 && registryName.startsWith("get") && it.parameterCount == 1 -> {
                    type.getters[registryName.drop(3).decapitalize()] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, clazz))
                }
                registryName.length > 3 && registryName.startsWith("set") && it.parameterCount == 2 -> {
                    type.setters[registryName.drop(3).decapitalize()] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                registryName.length > 2 && registryName.startsWith("to") && it.parameterCount == 1 -> {
                    type.autoCaster[it.returnType] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, clazz))
                }
                else -> UNARY_OPS[registryName]?.let { op ->
                    if (it.parameterCount == 1)
                        type.unaryOps[op] =
                            loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, clazz))
                } ?: BINARY_OPS[registryName]?.let { op ->
                    if (it.parameterCount == 2)
                        type.binaryOps[op] =
                            loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
            }
        }
    }

    inline fun <reified T> registryType() = registerType(T::class.java)

    fun register(clazz: Class<*>, path: String, name: String? = null) {
        registry[name ?: clazz.simpleName, path] = clazz
        registerType(clazz)
    }

    fun registerExtension(clazz: Class<*>, extending: Class<*>) {
        val loopUp = MethodHandles.lookup()
        val type = types[extending] ?: error("Tried to extended unregistered type for class: $extending")
        clazz.methods.forEach {
            if (!Modifier.isPublic(it.modifiers) || it.isAnnotationPresent(Palm.Ignore::class.java) || !Modifier.isStatic(
                    it.modifiers
                ) || it.parameterCount < 1 || it.parameterTypes.first() != extending
            ) return@forEach
            val registryName = it.getAnnotation(Palm.Name::class.java)?.name ?: it.name
            when {
                registryName == "get" && it.parameters.size > 1 -> {
                    type.multiOps[Get] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                registryName == "rangeTo" && it.parameters.size in 2..3 -> {
                    type.multiOps[ToRange] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                registryName.length > 3 && registryName.startsWith("get") && it.parameters.size == 1 -> {
                    type.getters[registryName] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, extending))
                }
                registryName.length > 3 && registryName.startsWith("set") && it.parameters.size == 2 -> {
                    type.setters[registryName] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                registryName.length > 2 && registryName.startsWith("to") && it.parameters.size == 1 -> {
                    type.autoCaster[it.returnType] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, extending))
                }
                else -> UNARY_OPS[registryName]?.let { op ->
                    if (it.parameters.size == 1)
                        type.unaryOps[op] =
                            loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, extending))
                } ?: BINARY_OPS[registryName]?.let { op ->
                    if (it.parameters.size == 2)
                        type.binaryOps[op] =
                            loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
            }
        }
    }
}