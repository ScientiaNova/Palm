package com.scientianovateam.palm.registry

import com.scientianovateam.palm.parser.BINARY_OPS
import com.scientianovateam.palm.parser.Get
import com.scientianovateam.palm.parser.ToRange
import com.scientianovateam.palm.parser.UNARY_OPS
import com.scientianovateam.palm.util.HashBasedTable
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Modifier
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

object TypeRegistry {
    private val NAMES = HashBasedTable<String, String, Class<*>>()

    fun classFromName(name: String, path: String? = null) =
        if (path.isNullOrBlank()) NAMES[name]?.values?.firstOrNull() else NAMES[name, path]

    private val TYPES = hashMapOf<Class<*>, IPalmType>()

    init {
        getOrRegister(Any::class.java, "base.any")
        getOrRegister(Number::class.java, path = "base")
        getOrRegister(Byte::class.java, path = "base")
        getOrRegister(Int::class.java, "base.int")
        getOrRegister(Float::class.java, path = "base")
        getOrRegister(Double::class.java, path = "base")
        getOrRegister(Long::class.java, path = "base")
        getOrRegister(Boolean::class.java, "base.bool")
        getOrRegister(Char::class.java, "base.char")
        getOrRegister(CharSequence::class.java, path = "base")
        getOrRegister(String::class.java, path = "base")
        getOrRegister(Stack::class.java, path = "base")
        getOrRegister(Queue::class.java, path = "base")
        getOrRegister(List::class.java, path = "base")
        getOrRegister(ArrayList::class.java, path = "base")
        getOrRegister(Set::class.java, path = "base")
        getOrRegister(TreeSet::class.java, path = "base")
        getOrRegister(HashSet::class.java, path = "base")
        getOrRegister(LinkedHashSet::class.java, path = "base")
        getOrRegister(Map::class.java, "base.dict")
        getOrRegister(Map.Entry::class.java, "base.dict_entry")
        getOrRegister(TreeMap::class.java, "base.tree_dict")
        getOrRegister(HashMap::class.java, "base.hash_dict")
        getOrRegister(LinkedHashMap::class.java, "base.linked_hash_dict")
        getOrRegister(Pair::class.java, path = "base")
        getOrRegister(IntRange::class.java, path = "base")
        getOrRegister(CharRange::class.java, path = "base")
        getOrRegister(LongRange::class.java, path = "base")
        getOrRegister(Random::class.java, path = "base")
        getOrRegister(Nothing::class.java, path = "base")
    }

    fun register(type: IPalmType) {
        val name = type.name.takeLastWhile { it != '.' }
        NAMES[name, type.name.dropLast(name.length + 1)] = type.clazz
        TYPES[type.clazz] = type
    }

    inline fun <reified T> getOrRegister() = getOrRegister(T::class.java)

    fun getOrRegister(clazz: Class<*>) =
        getOrRegister(clazz, (clazz.getAnnotation(Palm.Name::class.java)?.name ?: clazz.canonicalName).toSnakeCase())

    fun getOrRegister(clazz: Class<*>, canonicalName: String): IPalmType {
        TYPES[clazz]?.let { return it }
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

        val type = PalmType(canonicalName, clazz, constructor)
        register(type)

        clazz.declaredMethods.forEach {
            if (!Modifier.isPublic(it.modifiers) || it.isAnnotationPresent(Palm.Ignore::class.java) ||
                Modifier.isStatic(it.modifiers)
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
                it.returnType == Boolean::class.java && it.parameterCount == 1 && registryName !in UNARY_OPS -> {
                    type.getters[registryName] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, clazz))
                }
                registryName.length > 3 && registryName.startsWith("set") && it.parameterCount == 2 -> {
                    type.setters[registryName.drop(3).decapitalize()] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                registryName.length > 2 && registryName.startsWith("to") && it.parameterCount == 1 -> {
                    type.autoCasters[it.returnType] =
                        loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, clazz))
                }
                (registryName == "iter" || registryName == "iterator") && it.parameterCount == 1 &&
                        it.returnType.kotlin.isSubclassOf(Iterator::class) -> {
                    type.iterator = loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, clazz))
                }
                else -> UNARY_OPS[registryName]?.let { op ->
                    if (it.parameterCount == 1)
                        type.unaryOps[op] =
                            loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, clazz))
                } ?: BINARY_OPS[registryName]?.let { op ->
                    if (it.parameterCount == 2 && op.returnType?.equals(it.returnType) != false)
                        type.binaryOps[op] =
                            loopUp.findVirtual(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
            }
        }

        clazz.declaredFields.forEach {
            if (!Modifier.isPublic(it.modifiers) || it.isAnnotationPresent(Palm.Ignore::class.java) ||
                Modifier.isStatic(it.modifiers)
            ) return@forEach
            val registryName = it.getAnnotation(Palm.Name::class.java)?.name ?: it.name
            type.getters[registryName] = loopUp.findGetter(clazz, it.name, it.type)
            if (!Modifier.isFinal(it.modifiers))
                type.setters[registryName] = loopUp.findSetter(clazz, it.name, it.type)
        }

        return type
    }

    fun getOrRegister(clazz: Class<*>, path: String, name: String = clazz.simpleName) =
        getOrRegister(clazz, "$path.$name")

    fun registerExtension(clazz: Class<*>, extending: Class<*>) {
        val loopUp = MethodHandles.lookup()
        val type = TYPES[extending] as? PalmType ?: error("Tried to extended unregistered type for class: $extending")
        clazz.declaredMethods.forEach {
            if (!Modifier.isPublic(it.modifiers) || it.isAnnotationPresent(Palm.Ignore::class.java) || !Modifier.isStatic(
                    it.modifiers
                ) || it.parameterCount < 1 || it.parameterTypes.first() != extending
            ) return@forEach
            val registryName = it.getAnnotation(Palm.Name::class.java)?.name ?: it.name
            when {
                registryName == "get" && it.parameters.size > 1 -> {
                    type.multiOps[Get] =
                        loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                registryName == "rangeTo" && it.parameters.size in 2..3 -> {
                    type.multiOps[ToRange] =
                        loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                registryName.length > 3 && registryName.startsWith("get") && it.parameters.size == 1 -> {
                    type.getters[registryName] =
                        loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, extending))
                }
                it.returnType == Boolean::class.java && it.parameterCount == 1 && registryName !in UNARY_OPS -> {
                    type.getters[registryName] =
                        loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, extending))
                }
                registryName.length > 3 && registryName.startsWith("set") && it.parameters.size == 2 -> {
                    type.setters[registryName] =
                        loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                registryName.length > 2 && registryName.startsWith("to") && it.parameters.size == 1 -> {
                    type.autoCasters[it.returnType] =
                        loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, extending))
                }
                (registryName == "iter" || registryName == "iterator") && it.parameterCount == 1 &&
                        it.returnType.kotlin.isSubclassOf(Iterator::class) -> {
                    type.iterator = loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, extending))
                }
                else -> UNARY_OPS[registryName]?.let { op ->
                    if (it.parameters.size == 1)
                        type.unaryOps[op] =
                            loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, extending))
                } ?: BINARY_OPS[registryName]?.let { op ->
                    if (it.parameters.size == 2 && op.returnType?.equals(it.returnType) != false)
                        type.binaryOps[op] =
                            loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
            }
        }
    }

    fun String.toSnakeCase() =
        replace(""".\p{Lu}""".toRegex()) { it.value.last().toLowerCase().toString() }
            .replace("""\p{Lu}""".toRegex()) { "_" + it.value.first().toLowerCase() }
}