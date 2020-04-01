package com.scientianova.palm.registry

import com.scientianova.palm.evaluator.Scope
import com.scientianova.palm.evaluator.callVirtual
import com.scientianova.palm.tokenizer.StringTraverser
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.*
import kotlin.math.floor
import kotlin.math.pow

object TypeRegistry {
    private val PATH_REPLACEMENT = hashMapOf<String, String>()
    fun addPathReplacement(java: String, palm: String) {
        if (java !in PATH_REPLACEMENT) PATH_REPLACEMENT[java] = palm
    }

    fun replaceJavaPath(path: String) = PATH_REPLACEMENT[path] ?: path

    private val TYPES = hashMapOf<Class<*>, IPalmType>()

    init {
        addPathReplacement("java.util", "base")
        addPathReplacement("java.lang", "base")
        addPathReplacement("java.math", "base")
        addPathReplacement("kotlin", "base")
        addPathReplacement("kotlin.ranges", "base")
        addPathReplacement("kotlin.collections", "base")
        addPathReplacement("kotlin.text", "base")

        register(Any::class.java, "base", "any")
        register(object : PalmType(listOf("base", "number"), Number::class.java) {
            init {
                populate()
                val loopUp = MethodHandles.publicLookup()

                val byte = loopUp.findVirtual(clazz, "byteValue", MethodType.methodType(Byte::class.java))
                virtualCasters[Byte::class.javaPrimitiveType!!] = byte
                virtualCasters[Byte::class.javaObjectType] = byte

                val short = loopUp.findVirtual(clazz, "shortValue", MethodType.methodType(Short::class.java))
                virtualCasters[Short::class.javaPrimitiveType!!] = short
                virtualCasters[Short::class.javaObjectType] = short

                val int = loopUp.findVirtual(clazz, "intValue", MethodType.methodType(Int::class.java))
                virtualCasters[Int::class.javaPrimitiveType!!] = int
                virtualCasters[Int::class.javaObjectType] = int

                val long = loopUp.findVirtual(clazz, "longValue", MethodType.methodType(Long::class.java))
                virtualCasters[Long::class.javaPrimitiveType!!] = long
                virtualCasters[Long::class.javaObjectType] = long

                val float = loopUp.findVirtual(clazz, "floatValue", MethodType.methodType(Float::class.java))
                virtualCasters[Float::class.javaPrimitiveType!!] = float
                virtualCasters[Float::class.javaObjectType] = float

                val double = loopUp.findVirtual(clazz, "doubleValue", MethodType.methodType(Double::class.java))
                virtualCasters[Double::class.javaPrimitiveType!!] = double
                virtualCasters[Double::class.javaObjectType] = double
            }

            override fun callVirtual(name: String, scope: Scope, obj: Any?, args: List<Any?>) =
                if (args.size == 1 && args.first() is Number) when (name) {
                    "pow" -> (obj as Number).toDouble().pow((args.first() as Number).toDouble())
                    "floor_div" -> floor(obj.callVirtual("div", scope, args) as Double)
                    else -> super.callVirtual(name, scope, obj, args)
                } else super.callVirtual(name, scope, obj, args)
        })
        register(object : PalmType(listOf("base", "byte"), Byte::class.javaObjectType) {
            init {
                populate()
            }

            override fun callVirtual(name: String, scope: Scope, obj: Any?, args: List<Any?>): Any? {
                obj as Byte
                return when (args.size) {
                    0 -> when (name) {
                        "unary_minus" -> -obj
                        "unary_plus" -> obj
                        "inv" -> obj.toInt().inv().toByte()
                        else -> super.callVirtual(name, scope, obj, args)
                    }
                    1 -> {
                        val second = args[0]
                        if (second is Number) when (name) {
                            "plus" -> when (second) {
                                is Byte -> obj + second
                                is Short -> obj + second
                                is Int -> obj + second
                                is Long -> obj + second
                                is Float -> obj + second
                                is Double -> obj + second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "minus" -> when (second) {
                                is Byte -> obj - second
                                is Short -> obj - second
                                is Int -> obj - second
                                is Long -> obj - second
                                is Float -> obj - second
                                is Double -> obj - second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "mul" -> when (second) {
                                is Byte -> obj * second
                                is Short -> obj * second
                                is Int -> obj * second
                                is Long -> obj * second
                                is Float -> obj * second
                                is Double -> obj * second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "div" -> when (second) {
                                is Byte -> obj.toDouble() / second
                                is Short -> obj.toDouble() / second
                                is Int -> obj.toDouble() / second
                                is Long -> obj.toDouble() / second
                                is Float -> obj / second
                                is Double -> obj / second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "rem" -> when (second) {
                                is Byte -> obj.toDouble() % second
                                is Short -> obj.toDouble() % second
                                is Int -> obj.toDouble() % second
                                is Long -> obj.toDouble() % second
                                is Float -> obj % second
                                is Double -> obj % second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "shl" -> (obj.toInt() shl second.toInt()).toByte()
                            "shr" -> (obj.toInt() shr second.toInt()).toByte()
                            "ushr" -> (obj.toInt() ushr second.toInt()).toByte()
                            "or" -> (obj.toInt() or second.toInt()).toByte()
                            "and" -> (obj.toInt() and second.toInt()).toByte()
                            "range_to" -> if (second.toInt() >= obj) obj..second.toInt() else obj downTo second.toInt()
                            else -> super.callVirtual(name, scope, obj, args)
                        } else super.callVirtual(name, scope, obj, args)
                    }
                    2 -> if (args[0] is Number && args[1] is Number) IntProgression.fromClosedRange(
                        obj.toInt(), (args[1] as Number).toInt(), (args[0] as Number).toInt() - obj
                    ) else super.callVirtual(name, scope, obj, args)
                    else -> super.callVirtual(name, scope, obj, args)
                }
            }
        })
        register(object : PalmType(listOf("base", "short"), Short::class.javaObjectType) {
            init {
                populate()
            }

            override fun callVirtual(name: String, scope: Scope, obj: Any?, args: List<Any?>): Any? {
                obj as Short
                return when (args.size) {
                    0 -> when (name) {
                        "unary_minus" -> -obj
                        "unary_plus" -> obj
                        "inv" -> obj.toInt().inv().toShort()
                        else -> super.callVirtual(name, scope, obj, args)
                    }
                    1 -> {
                        val second = args[0]
                        if (second is Number) when (name) {
                            "plus" -> when (second) {
                                is Byte -> obj + second
                                is Short -> obj + second
                                is Int -> obj + second
                                is Long -> obj + second
                                is Float -> obj + second
                                is Double -> obj + second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "minus" -> when (second) {
                                is Byte -> obj - second
                                is Short -> obj - second
                                is Int -> obj - second
                                is Long -> obj - second
                                is Float -> obj - second
                                is Double -> obj - second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "mul" -> when (second) {
                                is Byte -> obj * second
                                is Short -> obj * second
                                is Int -> obj * second
                                is Long -> obj * second
                                is Float -> obj * second
                                is Double -> obj * second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "div" -> when (second) {
                                is Byte -> obj.toDouble() / second
                                is Short -> obj.toDouble() / second
                                is Int -> obj.toDouble() / second
                                is Long -> obj.toDouble() / second
                                is Float -> obj / second
                                is Double -> obj / second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "rem" -> when (second) {
                                is Byte -> obj.toDouble() % second
                                is Short -> obj.toDouble() % second
                                is Int -> obj.toDouble() % second
                                is Long -> obj.toDouble() % second
                                is Float -> obj % second
                                is Double -> obj % second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "shl" -> (obj.toInt() shl second.toInt()).toShort()
                            "shr" -> (obj.toInt() shr second.toInt()).toShort()
                            "ushr" -> (obj.toInt() ushr second.toInt()).toShort()
                            "or" -> (obj.toInt() or second.toInt()).toShort()
                            "and" -> (obj.toInt() and second.toInt()).toShort()
                            "range_to" -> if (second.toInt() >= obj) obj..second.toInt() else obj downTo second.toInt()
                            else -> super.callVirtual(name, scope, obj, args)
                        } else super.callVirtual(name, scope, obj, args)
                    }
                    2 -> if (args[0] is Number && args[1] is Number) IntProgression.fromClosedRange(
                        obj.toInt(), (args[1] as Number).toInt(), (args[0] as Number).toInt() - obj
                    ) else super.callVirtual(name, scope, obj, args)
                    else -> super.callVirtual(name, scope, obj, args)
                }
            }
        })
        register(object : PalmType(listOf("base", "int"), Int::class.javaObjectType) {
            init {
                populate()
            }

            override fun callVirtual(name: String, scope: Scope, obj: Any?, args: List<Any?>): Any? {
                obj as Int
                return when (args.size) {
                    0 -> when (name) {
                        "unary_minus" -> -obj
                        "unary_plus" -> obj
                        "inv" -> obj.inv()
                        else -> super.callVirtual(name, scope, obj, args)
                    }
                    1 -> {
                        val second = args[0]
                        if (second is Number) when (name) {
                            "plus" -> when (second) {
                                is Byte -> obj + second
                                is Short -> obj + second
                                is Int -> obj + second
                                is Long -> obj + second
                                is Float -> obj + second
                                is Double -> obj + second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "minus" -> when (second) {
                                is Byte -> obj - second
                                is Short -> obj - second
                                is Int -> obj - second
                                is Long -> obj - second
                                is Float -> obj - second
                                is Double -> obj - second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "mul" -> when (second) {
                                is Byte -> obj * second
                                is Short -> obj * second
                                is Int -> obj * second
                                is Long -> obj * second
                                is Float -> obj * second
                                is Double -> obj * second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "div" -> when (second) {
                                is Byte -> obj.toDouble() / second
                                is Short -> obj.toDouble() / second
                                is Int -> obj.toDouble() / second
                                is Long -> obj.toDouble() / second
                                is Float -> obj / second
                                is Double -> obj / second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "rem" -> when (second) {
                                is Byte -> obj.toDouble() % second
                                is Short -> obj.toDouble() % second
                                is Int -> obj.toDouble() % second
                                is Long -> obj.toDouble() % second
                                is Float -> obj % second
                                is Double -> obj % second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "shl" -> obj shl second.toInt()
                            "shr" -> obj shr second.toInt()
                            "ushr" -> obj ushr second.toInt()
                            "or" -> obj or second.toInt()
                            "and" -> obj and second.toInt()
                            "range_to" -> if (second.toInt() >= obj) obj..second.toInt() else obj downTo second.toInt()
                            else -> super.callVirtual(name, scope, obj, args)
                        } else super.callVirtual(name, scope, obj, args)
                    }
                    2 -> if (args[0] is Number && args[1] is Number) IntProgression.fromClosedRange(
                        obj, (args[1] as Number).toInt(), (args[0] as Number).toInt() - obj
                    ) else super.callVirtual(name, scope, obj, args)
                    else -> super.callVirtual(name, scope, obj, args)
                }
            }
        })
        register(object : PalmType(listOf("base", "long"), Long::class.javaObjectType) {
            init {
                populate()
            }

            override fun callVirtual(name: String, scope: Scope, obj: Any?, args: List<Any?>): Any? {
                obj as Long
                return when (args.size) {
                    0 -> when (name) {
                        "unary_minus" -> -obj
                        "unary_plus" -> obj
                        "inv" -> obj.inv()
                        else -> super.callVirtual(name, scope, obj, args)
                    }
                    1 -> {
                        val second = args[0]
                        if (second is Number) when (name) {
                            "plus" -> when (second) {
                                is Byte -> obj + second
                                is Short -> obj + second
                                is Int -> obj + second
                                is Long -> obj + second
                                is Float -> obj + second
                                is Double -> obj + second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "minus" -> when (second) {
                                is Byte -> obj - second
                                is Short -> obj - second
                                is Int -> obj - second
                                is Long -> obj - second
                                is Float -> obj - second
                                is Double -> obj - second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "mul" -> when (second) {
                                is Byte -> obj * second
                                is Short -> obj * second
                                is Int -> obj * second
                                is Long -> obj * second
                                is Float -> obj * second
                                is Double -> obj * second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "div" -> when (second) {
                                is Byte -> obj.toDouble() / second
                                is Short -> obj.toDouble() / second
                                is Int -> obj.toDouble() / second
                                is Long -> obj.toDouble() / second
                                is Float -> obj / second
                                is Double -> obj / second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "rem" -> when (second) {
                                is Byte -> obj.toDouble() % second
                                is Short -> obj.toDouble() % second
                                is Int -> obj.toDouble() % second
                                is Long -> obj.toDouble() % second
                                is Float -> obj % second
                                is Double -> obj % second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "shl" -> obj shl second.toInt()
                            "shr" -> obj shr second.toInt()
                            "ushr" -> obj ushr second.toInt()
                            "or" -> obj or second.toLong()
                            "and" -> obj and second.toLong()
                            "range_to" -> if (second.toLong() >= obj) obj..second.toLong() else obj downTo second.toLong()
                            else -> super.callVirtual(name, scope, obj, args)
                        } else super.callVirtual(name, scope, obj, args)
                    }
                    2 -> if (args[0] is Number && args[1] is Number) LongProgression.fromClosedRange(
                        obj, (args[1] as Number).toLong(), (args[0] as Number).toLong() - obj
                    ) else super.callVirtual(name, scope, obj, args)
                    else -> super.callVirtual(name, scope, obj, args)
                }
            }
        })
        register(object : PalmType(listOf("base", "float"), Float::class.javaObjectType) {
            init {
                populate()
            }

            override fun callVirtual(name: String, scope: Scope, obj: Any?, args: List<Any?>): Any? {
                obj as Float
                return when (args.size) {
                    0 -> when (name) {
                        "unary_minus" -> -obj
                        "unary_plus" -> obj
                        else -> super.callVirtual(name, scope, obj, args)
                    }
                    1 -> {
                        val second = args[0]
                        if (second is Number) when (name) {
                            "plus" -> when (second) {
                                is Byte -> obj + second
                                is Short -> obj + second
                                is Int -> obj + second
                                is Long -> obj + second
                                is Float -> obj + second
                                is Double -> obj + second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "minus" -> when (second) {
                                is Byte -> obj - second
                                is Short -> obj - second
                                is Int -> obj - second
                                is Long -> obj - second
                                is Float -> obj - second
                                is Double -> obj - second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "mul" -> when (second) {
                                is Byte -> obj * second
                                is Short -> obj * second
                                is Int -> obj * second
                                is Long -> obj * second
                                is Float -> obj * second
                                is Double -> obj * second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "div" -> when (second) {
                                is Byte -> obj / second
                                is Short -> obj / second
                                is Int -> obj / second
                                is Long -> obj / second
                                is Float -> obj / second
                                is Double -> obj / second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "rem" -> when (second) {
                                is Byte -> obj % second
                                is Short -> obj % second
                                is Int -> obj % second
                                is Long -> obj % second
                                is Float -> obj % second
                                is Double -> obj % second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            else -> super.callVirtual(name, scope, obj, args)
                        } else super.callVirtual(name, scope, obj, args)
                    }
                    else -> super.callVirtual(name, scope, obj, args)
                }
            }
        })
        register(object : PalmType(listOf("base", "double"), Double::class.javaObjectType) {
            init {
                populate()
            }

            override fun callVirtual(name: String, scope: Scope, obj: Any?, args: List<Any?>): Any? {
                obj as Double
                return when (args.size) {
                    0 -> when (name) {
                        "unary_minus" -> -obj
                        "unary_plus" -> obj
                        else -> super.callVirtual(name, scope, obj, args)
                    }
                    1 -> {
                        val second = args[0]
                        if (second is Number) when (name) {
                            "plus" -> when (second) {
                                is Byte -> obj + second
                                is Short -> obj + second
                                is Int -> obj + second
                                is Long -> obj + second
                                is Float -> obj + second
                                is Double -> obj + second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "minus" -> when (second) {
                                is Byte -> obj - second
                                is Short -> obj - second
                                is Int -> obj - second
                                is Long -> obj - second
                                is Float -> obj - second
                                is Double -> obj - second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "mul" -> when (second) {
                                is Byte -> obj * second
                                is Short -> obj * second
                                is Int -> obj * second
                                is Long -> obj * second
                                is Float -> obj * second
                                is Double -> obj * second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "div" -> when (second) {
                                is Byte -> obj / second
                                is Short -> obj / second
                                is Int -> obj / second
                                is Long -> obj / second
                                is Float -> obj / second
                                is Double -> obj / second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "rem" -> when (second) {
                                is Byte -> obj % second
                                is Short -> obj % second
                                is Int -> obj % second
                                is Long -> obj % second
                                is Float -> obj % second
                                is Double -> obj % second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            else -> super.callVirtual(name, scope, obj, args)
                        } else super.callVirtual(name, scope, obj, args)
                    }
                    else -> super.callVirtual(name, scope, obj, args)
                }
            }
        })
        register(Boolean::class.javaObjectType, "base", "bool")
        register(object : PalmType(listOf("base", "char"), Char::class.javaObjectType) {
            init {
                populate()
            }

            override fun callVirtual(name: String, scope: Scope, obj: Any?, args: List<Any?>): Any? {
                obj as Char
                return when (args.size) {
                    1 -> {
                        val second = args[0]
                        when (name) {
                            "plus" -> when (second) {
                                is Byte -> obj + second.toInt()
                                is Short -> obj + second.toInt()
                                is Int -> obj + second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "minus" -> when (second) {
                                is Byte -> obj - second.toInt()
                                is Short -> obj - second.toInt()
                                is Int -> obj - second
                                is Char -> obj - second
                                else -> super.callVirtual(name, scope, obj, args)
                            }
                            "range_to" ->
                                if (second is Char) if (second >= obj) obj..second else obj downTo second
                                else super.callVirtual(name, scope, obj, args)
                            else -> super.callVirtual(name, scope, obj, args)
                        }
                    }
                    2 -> if (args[0] is Char && args[1] is Char) CharProgression.fromClosedRange(
                        obj, args[1] as Char, args[0] as Char - obj
                    ) else super.callVirtual(name, scope, obj, args)
                    else -> super.callVirtual(name, scope, obj, args)
                }
            }
        })
        register(object : PalmType(listOf("base", "string"), String::class.java) {
            init {
                populate()
                virtual["get"] = MethodHandles.publicLookup().findVirtual(
                    String::class.java, "charAt",
                    MethodType.methodType(Char::class.java, Int::class.java)
                )
            }

            override fun callVirtual(name: String, scope: Scope, obj: Any?, args: List<Any?>) =
                if (args.size == 1 && name == "plus") (obj as String) + args[0]
                else super.callVirtual(name, scope, obj, args)
        })
        getOrRegister(java.lang.Math::class.java)
        getOrRegister<Iterator<*>>()
        getOrRegister<Iterable<*>>()
        getOrRegister<Collection<*>>()
        getOrRegister<AbstractCollection<*>>()
        getOrRegister<List<*>>()
        getOrRegister<AbstractList<*>>()
        getOrRegister<ArrayList<*>>()
        getOrRegister<LinkedList<*>>()
        getOrRegister<Vector<*>>()
        getOrRegister<Stack<*>>()
        getOrRegister<Queue<*>>()
        getOrRegister<PriorityQueue<*>>()
        getOrRegister<Deque<*>>()
        getOrRegister<Set<*>>()
        getOrRegister<AbstractSet<*>>()
        getOrRegister<HashSet<*>>()
        getOrRegister<LinkedHashSet<*>>()
        getOrRegister<TreeSet<*>>()
        getOrRegister<Map<*, *>>()
        getOrRegister<Map.Entry<*, *>>()
        getOrRegister<AbstractMap<*, *>>()
        getOrRegister<HashMap<*, *>>()
        getOrRegister<LinkedHashMap<*, *>>()
        getOrRegister<TreeMap<*, *>>()
        getOrRegister<Sequence<*>>()
        getOrRegister<Pair<*, *>>()
        getOrRegister<Triple<*, *, *>>()
        getOrRegister<IntRange>()
        getOrRegister<CharRange>()
        getOrRegister<LongRange>()
        getOrRegister<Random>()
        getOrRegister(Class.forName("kotlin.collections.CollectionsKt"))
        getOrRegister(Class.forName("kotlin.collections.ArraysKt"))
        getOrRegister(Class.forName("kotlin.collections.MapsKt"))
        getOrRegister(Class.forName("kotlin.collections.SetsKt"))
        getOrRegister(Class.forName("kotlin.ranges.RangesKt"))
        getOrRegister(Class.forName("kotlin.text.StringsKt"))
    }

    fun register(type: IPalmType) {
        RootPathNode.addType(type.name, type).also { node ->
            if (type.name.first() == "base") {
                if (type.clazz.declaredConstructors.isNotEmpty())
                    Scope.GLOBAL.imports[type.name.last()] = node
                for ((name, functions) in type.getAllStatic()) if (name != "new")
                    functions.forEach { Scope.GLOBAL.addStaticImport(name, it) }
            }
        }
        TYPES[type.clazz] = type
    }

    inline fun <reified T> getOrRegister() = getOrRegister(T::class.java)

    fun getOrRegister(clazz: Class<*>) = register(
        clazz, clazz.getAnnotation(Palm.Name::class.java)?.name?.split('.') ?: clazz.typeName.toList()
    )

    fun register(clazz: Class<*>, vararg path: String) = register(clazz, path.toList())

    fun register(clazz: Class<*>, path: List<String>): IPalmType {
        TYPES[clazz]?.let { return it }
        val type = PalmType(path, clazz)
        if (!clazz.isInterface) type.populate()
        register(type)
        return type
    }
}

fun String.toList(): List<String> {
    val name = substringAfterLast(".")
    val path = TypeRegistry.replaceJavaPath(dropLast(name.length + 1))
    return path.split('.') + name.toSnakeCase()
}

fun String.toSnakeCase(): String {
    val traverser = StringTraverser(this, "")
    val builder = StringBuilder().append(traverser.pop()?.toLowerCase())
    loop@ while (true) {
        val current = traverser.pop()
        when {
            current == null ->
                break@loop
            current == '.' && traverser.peek()?.isUpperCase() == true ->
                builder.append(".${traverser.pop()!!.toLowerCase()}")
            current.isUpperCase() ->
                builder.append("_${current.toLowerCase()}")
            current == '$' -> continue@loop
            else -> builder.append(current)
        }
    }
    return builder.toString()
}