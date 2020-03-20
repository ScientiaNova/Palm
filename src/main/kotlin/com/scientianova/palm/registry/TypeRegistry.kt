package com.scientianova.palm.registry

import com.scientianova.palm.evaluator.palmType
import com.scientianova.palm.parser.*
import com.scientianova.palm.tokenizer.StringTraverser
import com.scientianova.palm.util.HashBasedTable
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Modifier
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashSet
import kotlin.math.*

object TypeRegistry {
    private val PATH_REPLACEMENT = hashMapOf<String, String>()
    fun addPathReplacement(java: String, palm: String) {
        if (java !in PATH_REPLACEMENT) PATH_REPLACEMENT[java] = palm
    }

    fun replaceJavaPath(path: String) = PATH_REPLACEMENT[path] ?: path

    private val NAMES = HashBasedTable<String, String, Class<*>>()

    fun classFromName(name: String, path: String? = null) =
        if (path.isNullOrBlank()) NAMES[name]?.values?.firstOrNull() else NAMES[name, path]

    private val TYPES = hashMapOf<Class<*>, IPalmType>()

    init {
        addPathReplacement("java.util", "base")
        addPathReplacement("java.lang", "base")
        addPathReplacement("java.math", "base")
        addPathReplacement("kotlin", "base")
        addPathReplacement("kotlin.ranges", "base")

        register(Any::class.java, "base", "any")
        register(object : PalmType(TypeName("base", "number"), Number::class.java) {
            init {
                populate()
                val loopUp = MethodHandles.publicLookup()
                autoCasters[Byte::class.java] =
                    loopUp.findVirtual(clazz, "byteValue", MethodType.methodType(Byte::class.java))
                autoCasters[Short::class.java] =
                    loopUp.findVirtual(clazz, "shortValue", MethodType.methodType(Short::class.java))
                autoCasters[Int::class.java] =
                    loopUp.findVirtual(clazz, "intValue", MethodType.methodType(Int::class.java))
                autoCasters[Long::class.java] =
                    loopUp.findVirtual(clazz, "longValue", MethodType.methodType(Long::class.java))
                autoCasters[Float::class.java] =
                    loopUp.findVirtual(clazz, "floatValue", MethodType.methodType(Float::class.java))
                autoCasters[Double::class.java] =
                    loopUp.findVirtual(clazz, "doubleValue", MethodType.methodType(Double::class.java))
            }

            override fun execute(op: BinaryOperation, obj: Any?, second: Any?) = when {
                op == Pow && second is Number -> (obj as Number).toDouble().pow(second.toDouble())
                op == FloorDiv && second is Number -> floor(obj.palmType.execute(Div, obj, second) as Double)
                else -> super.execute(op, obj, second)
            }

            override fun get(obj: Any?, name: String): Any? {
                obj as Number
                return when (name) {
                    "ceil_value" -> ceil(obj.toDouble())
                    "floor_value" -> floor(obj.toDouble())
                    "rounded" -> round(obj.toDouble())
                    "sin" -> sin(obj.toDouble())
                    "cos" -> cos(obj.toDouble())
                    "tan" -> tan(obj.toDouble())
                    "sinh" -> sinh(obj.toDouble())
                    "cosh" -> cosh(obj.toDouble())
                    "tanh" -> tanh(obj.toDouble())
                    "asin" -> asin(obj.toDouble())
                    "acos" -> acos(obj.toDouble())
                    "atan" -> atan(obj.toDouble())
                    "asinh" -> asinh(obj.toDouble())
                    "acosh" -> acosh(obj.toDouble())
                    "atanh" -> atanh(obj.toDouble())
                    "ln" -> ln(obj.toDouble())
                    "log2" -> log2(obj.toDouble())
                    "log10" -> log10(obj.toDouble())
                    "exp" -> exp(obj.toDouble())
                    else -> super.get(obj, name)
                }
            }
        })
        register(object : PalmType(TypeName("base", "byte"), Byte::class.javaObjectType) {
            init {
                populate()
            }

            override fun execute(op: UnaryOperation, obj: Any?): Any {
                obj as Byte
                return when (op) {
                    is UnaryMinus -> -obj
                    is UnaryPlus -> obj
                    is Inv -> obj.toInt().inv().toByte()
                    is Not -> super.execute(op, obj)
                }
            }

            override fun execute(op: BinaryOperation, obj: Any?, second: Any?): Any {
                obj as Byte
                return when (op) {
                    is Plus -> when (second) {
                        is Byte -> obj + second
                        is Short -> obj + second
                        is Int -> obj + second
                        is Long -> obj + second
                        is Float -> obj + second
                        is Double -> obj + second
                        else -> super.execute(op, obj, second)
                    }
                    is Minus -> when (second) {
                        is Byte -> obj - second
                        is Short -> obj - second
                        is Int -> obj - second
                        is Long -> obj - second
                        is Float -> obj - second
                        is Double -> obj - second
                        else -> super.execute(op, obj, second)
                    }
                    is Mul -> when (second) {
                        is Byte -> obj * second
                        is Short -> obj * second
                        is Int -> obj * second
                        is Long -> obj * second
                        is Float -> obj * second
                        is Double -> obj * second
                        else -> super.execute(op, obj, second)
                    }
                    is Div -> when (second) {
                        is Byte -> obj.toDouble() / second
                        is Short -> obj.toDouble() / second
                        is Int -> obj.toDouble() / second
                        is Long -> obj.toDouble() / second
                        is Float -> obj / second
                        is Double -> obj / second
                        else -> super.execute(op, obj, second)
                    }
                    is Rem -> when (second) {
                        is Byte -> obj.toDouble() % second
                        is Short -> obj.toDouble() % second
                        is Int -> obj.toDouble() % second
                        is Long -> obj.toDouble() % second
                        is Float -> obj % second
                        is Double -> obj % second
                        else -> super.execute(op, obj, second)
                    }
                    is Shl ->
                        if (second is Number) (obj.toInt() shl second.toInt()).toByte()
                        else super.execute(op, obj, second)
                    is Shr ->
                        if (second is Number) (obj.toInt() shr second.toInt()).toByte()
                        else super.execute(op, obj, second)
                    is Ushr ->
                        if (second is Number) (obj.toInt() ushr second.toInt()).toByte()
                        else super.execute(op, obj, second)
                    is Or ->
                        if (second is Number) (obj.toInt() or second.toInt()).toByte()
                    else super.execute(op, obj, second)
                    is And ->
                        if (second is Number) (obj.toInt() and second.toInt()).toByte()
                        else super.execute(op, obj, second)
                    else -> super.execute(op, obj, second)
                }
            }

            override fun execute(op: MultiOperation, obj: Any?, rest: List<Any?>): Any {
                obj as Byte
                val second = rest.first()
                return if (op == RangeTo && second is Number) when (val third = rest.getOrNull(1)) {
                    null -> if (second.toInt() >= obj) obj..second.toInt() else obj downTo second.toInt()
                    is Number -> IntProgression.fromClosedRange(obj.toInt(), third.toInt(), second.toInt() - obj)
                    else -> super.execute(op, obj, rest)
                } else super.execute(op, obj, rest)
            }
        })
        register(object : PalmType(TypeName("base", "short"), Short::class.javaObjectType) {
            init {
                populate()
            }

            override fun execute(op: UnaryOperation, obj: Any?): Any {
                obj as Short
                return when (op) {
                    is UnaryMinus -> -obj
                    is UnaryPlus -> obj
                    is Inv -> obj.toInt().inv().toShort()
                    is Not -> super.execute(op, obj)
                }
            }

            override fun execute(op: BinaryOperation, obj: Any?, second: Any?): Any {
                obj as Short
                return when (op) {
                    is Plus -> when (second) {
                        is Byte -> obj + second
                        is Short -> obj + second
                        is Int -> obj + second
                        is Long -> obj + second
                        is Float -> obj + second
                        is Double -> obj + second
                        else -> super.execute(op, obj, second)
                    }
                    is Minus -> when (second) {
                        is Byte -> obj - second
                        is Short -> obj - second
                        is Int -> obj - second
                        is Long -> obj - second
                        is Float -> obj - second
                        is Double -> obj - second
                        else -> super.execute(op, obj, second)
                    }
                    is Mul -> when (second) {
                        is Byte -> obj * second
                        is Short -> obj * second
                        is Int -> obj * second
                        is Long -> obj * second
                        is Float -> obj * second
                        is Double -> obj * second
                        else -> super.execute(op, obj, second)
                    }
                    is Div -> when (second) {
                        is Byte -> obj.toDouble() / second
                        is Short -> obj.toDouble() / second
                        is Int -> obj.toDouble() / second
                        is Long -> obj.toDouble() / second
                        is Float -> obj / second
                        is Double -> obj / second
                        else -> super.execute(op, obj, second)
                    }
                    is Rem -> when (second) {
                        is Byte -> obj.toDouble() % second
                        is Short -> obj.toDouble() % second
                        is Int -> obj.toDouble() % second
                        is Long -> obj.toDouble() % second
                        is Float -> obj % second
                        is Double -> obj % second
                        else -> super.execute(op, obj, second)
                    }
                    is Shl ->
                        if (second is Number) (obj.toInt() shl second.toInt()).toShort()
                        else super.execute(op, obj, second)
                    is Shr ->
                        if (second is Number) (obj.toInt() shr second.toInt()).toShort()
                        else super.execute(op, obj, second)
                    is Ushr ->
                        if (second is Number) (obj.toInt() ushr second.toInt()).toShort()
                        else super.execute(op, obj, second)
                    is Or ->
                        if (second is Number) (obj.toInt() or second.toInt()).toShort()
                        else super.execute(op, obj, second)
                    is And ->
                        if (second is Number) (obj.toInt() and second.toInt()).toShort()
                        else super.execute(op, obj, second)
                    else -> super.execute(op, obj, second)
                }
            }

            override fun execute(op: MultiOperation, obj: Any?, rest: List<Any?>): Any {
                obj as Short
                val second = rest.first()
                return if (op == RangeTo && second is Number) when (val third = rest.getOrNull(1)) {
                    null -> if (second.toInt() >= obj) obj..second.toInt() else obj downTo second.toInt()
                    is Number -> IntProgression.fromClosedRange(obj.toInt(), third.toInt(), second.toInt() - obj)
                    else -> super.execute(op, obj, rest)
                } else super.execute(op, obj, rest)
            }
        })
        register(object : PalmType(TypeName("base", "int"), Int::class.javaObjectType) {
            init {
                populate()
            }

            override fun execute(op: UnaryOperation, obj: Any?): Any {
                obj as Int
                return when (op) {
                    is UnaryMinus -> -obj
                    is UnaryPlus -> obj
                    is Inv -> obj.inv()
                    is Not -> super.execute(op, obj)
                }
            }

            override fun execute(op: BinaryOperation, obj: Any?, second: Any?): Any {
                obj as Int
                return when (op) {
                    is Plus -> when (second) {
                        is Byte -> obj + second
                        is Short -> obj + second
                        is Int -> obj + second
                        is Long -> obj + second
                        is Float -> obj + second
                        is Double -> obj + second
                        else -> super.execute(op, obj, second)
                    }
                    is Minus -> when (second) {
                        is Byte -> obj - second
                        is Short -> obj - second
                        is Int -> obj - second
                        is Long -> obj - second
                        is Float -> obj - second
                        is Double -> obj - second
                        else -> super.execute(op, obj, second)
                    }
                    is Mul -> when (second) {
                        is Byte -> obj * second
                        is Short -> obj * second
                        is Int -> obj * second
                        is Long -> obj * second
                        is Float -> obj * second
                        is Double -> obj * second
                        else -> super.execute(op, obj, second)
                    }
                    is Div -> when (second) {
                        is Byte -> obj.toDouble() / second
                        is Short -> obj.toDouble() / second
                        is Int -> obj.toDouble() / second
                        is Long -> obj.toDouble() / second
                        is Float -> obj / second
                        is Double -> obj / second
                        else -> super.execute(op, obj, second)
                    }
                    is Rem -> when (second) {
                        is Byte -> obj.toDouble() % second
                        is Short -> obj.toDouble() % second
                        is Int -> obj.toDouble() % second
                        is Long -> obj.toDouble() % second
                        is Float -> obj % second
                        is Double -> obj % second
                        else -> super.execute(op, obj, second)
                    }
                    is Shl ->
                        if (second is Number) (obj shl second.toInt())
                        else super.execute(op, obj, second)
                    is Shr ->
                        if (second is Number) (obj shr second.toInt())
                        else super.execute(op, obj, second)
                    is Ushr ->
                        if (second is Number) (obj ushr second.toInt())
                        else super.execute(op, obj, second)
                    is Or ->
                        if (second is Number) (obj or second.toInt())
                        else super.execute(op, obj, second)
                    is And ->
                        if (second is Number) (obj and second.toInt())
                        else super.execute(op, obj, second)
                    else -> super.execute(op, obj, second)
                }
            }

            override fun execute(op: MultiOperation, obj: Any?, rest: List<Any?>): Any {
                obj as Int
                val second = rest.first()
                return if (op == RangeTo && second is Number) when (val third = rest.getOrNull(1)) {
                    null -> if (second.toInt() >= obj) obj..second.toInt() else obj downTo second.toInt()
                    is Number -> IntProgression.fromClosedRange(obj, third.toInt(), second.toInt() - obj)
                    else -> super.execute(op, obj, rest)
                } else super.execute(op, obj, rest)
            }
        })
        register(object : PalmType(TypeName("base", "long"), Long::class.javaObjectType) {
            init {
                populate()
            }

            override fun execute(op: UnaryOperation, obj: Any?): Any {
                obj as Long
                return when (op) {
                    is UnaryMinus -> -obj
                    is UnaryPlus -> obj
                    is Inv -> obj.inv()
                    is Not -> super.execute(op, obj)
                }
            }

            override fun execute(op: BinaryOperation, obj: Any?, second: Any?): Any {
                obj as Long
                return when (op) {
                    is Plus -> when (second) {
                        is Byte -> obj + second
                        is Short -> obj + second
                        is Int -> obj + second
                        is Long -> obj + second
                        is Float -> obj + second
                        is Double -> obj + second
                        else -> super.execute(op, obj, second)
                    }
                    is Minus -> when (second) {
                        is Byte -> obj - second
                        is Short -> obj - second
                        is Int -> obj - second
                        is Long -> obj - second
                        is Float -> obj - second
                        is Double -> obj - second
                        else -> super.execute(op, obj, second)
                    }
                    is Mul -> when (second) {
                        is Byte -> obj * second
                        is Short -> obj * second
                        is Int -> obj * second
                        is Long -> obj * second
                        is Float -> obj * second
                        is Double -> obj * second
                        else -> super.execute(op, obj, second)
                    }
                    is Div -> when (second) {
                        is Byte -> obj.toDouble() / second
                        is Short -> obj.toDouble() / second
                        is Int -> obj.toDouble() / second
                        is Long -> obj.toDouble() / second
                        is Float -> obj / second
                        is Double -> obj / second
                        else -> super.execute(op, obj, second)
                    }
                    is Rem -> when (second) {
                        is Byte -> obj.toDouble() % second
                        is Short -> obj.toDouble() % second
                        is Int -> obj.toDouble() % second
                        is Long -> obj.toDouble() % second
                        is Float -> obj % second
                        is Double -> obj % second
                        else -> super.execute(op, obj, second)
                    }
                    is Shl ->
                        if (second is Number) (obj shl second.toInt())
                        else super.execute(op, obj, second)
                    is Shr ->
                        if (second is Number) (obj shr second.toInt())
                        else super.execute(op, obj, second)
                    is Ushr ->
                        if (second is Number) (obj ushr second.toInt())
                        else super.execute(op, obj, second)
                    is Or ->
                        if (second is Number) (obj or second.toLong())
                        else super.execute(op, obj, second)
                    is And ->
                        if (second is Number) (obj and second.toLong())
                        else super.execute(op, obj, second)
                    else -> super.execute(op, obj, second)
                }
            }

            override fun execute(op: MultiOperation, obj: Any?, rest: List<Any?>): Any {
                obj as Long
                val second = rest.first()
                return if (op == RangeTo && second is Number) when (val third = rest.getOrNull(1)) {
                    null -> if (second.toLong() >= obj) obj..second.toLong() else obj downTo second.toLong()
                    is Number -> LongProgression.fromClosedRange(obj, third.toLong(), second.toLong() - obj)
                    else -> super.execute(op, obj, rest)
                } else super.execute(op, obj, rest)
            }
        })
        register(object : PalmType(TypeName("base", "float"), Float::class.javaObjectType) {
            init {
                populate()
            }

            override fun execute(op: UnaryOperation, obj: Any?): Any {
                obj as Float
                return when (op) {
                    is UnaryMinus -> -obj
                    is UnaryPlus -> obj
                    is Inv -> obj.toInt().inv().toFloat()
                    is Not -> super.execute(op, obj)
                }
            }

            override fun execute(op: BinaryOperation, obj: Any?, second: Any?): Any {
                obj as Float
                return when (op) {
                    is Plus -> when (second) {
                        is Byte -> obj + second
                        is Short -> obj + second
                        is Int -> obj + second
                        is Long -> obj + second
                        is Float -> obj + second
                        is Double -> obj + second
                        else -> super.execute(op, obj, second)
                    }
                    is Minus -> when (second) {
                        is Byte -> obj - second
                        is Short -> obj - second
                        is Int -> obj - second
                        is Long -> obj - second
                        is Float -> obj - second
                        is Double -> obj - second
                        else -> super.execute(op, obj, second)
                    }
                    is Mul -> when (second) {
                        is Byte -> obj * second
                        is Short -> obj * second
                        is Int -> obj * second
                        is Long -> obj * second
                        is Float -> obj * second
                        is Double -> obj * second
                        else -> super.execute(op, obj, second)
                    }
                    is Div -> when (second) {
                        is Byte -> obj / second
                        is Short -> obj / second
                        is Int -> obj / second
                        is Long -> obj / second
                        is Float -> obj / second
                        is Double -> obj / second
                        else -> super.execute(op, obj, second)
                    }
                    is Rem -> when (second) {
                        is Byte -> obj % second
                        is Short -> obj % second
                        is Int -> obj % second
                        is Long -> obj % second
                        is Float -> obj % second
                        is Double -> obj % second
                        else -> super.execute(op, obj, second)
                    }
                    is Shl ->
                        if (second is Number) (obj.toInt() shl second.toInt()).toFloat()
                        else super.execute(op, obj, second)
                    is Shr ->
                        if (second is Number) (obj.toInt() shr second.toInt()).toFloat()
                        else super.execute(op, obj, second)
                    is Ushr ->
                        if (second is Number) (obj.toInt() ushr second.toInt()).toFloat()
                        else super.execute(op, obj, second)
                    is Or ->
                        if (second is Number) (obj.toInt() or second.toInt()).toFloat()
                        else super.execute(op, obj, second)
                    is And ->
                        if (second is Number) (obj.toInt() and second.toInt()).toFloat()
                        else super.execute(op, obj, second)
                    else -> super.execute(op, obj, second)
                }
            }
        })
        register(object : PalmType(TypeName("base", "double"), Double::class.javaObjectType) {
            init {
                populate()
            }

            override fun execute(op: UnaryOperation, obj: Any?): Any {
                obj as Double
                return when (op) {
                    is UnaryMinus -> -obj
                    is UnaryPlus -> obj
                    is Inv -> obj.toLong().inv().toDouble()
                    is Not -> super.execute(op, obj)
                }
            }

            override fun execute(op: BinaryOperation, obj: Any?, second: Any?): Any {
                obj as Double
                return when (op) {
                    is Plus -> when (second) {
                        is Byte -> obj + second
                        is Short -> obj + second
                        is Int -> obj + second
                        is Long -> obj + second
                        is Float -> obj + second
                        is Double -> obj + second
                        else -> super.execute(op, obj, second)
                    }
                    is Minus -> when (second) {
                        is Byte -> obj - second
                        is Short -> obj - second
                        is Int -> obj - second
                        is Long -> obj - second
                        is Float -> obj - second
                        is Double -> obj - second
                        else -> super.execute(op, obj, second)
                    }
                    is Mul -> when (second) {
                        is Byte -> obj * second
                        is Short -> obj * second
                        is Int -> obj * second
                        is Long -> obj * second
                        is Float -> obj * second
                        is Double -> obj * second
                        else -> super.execute(op, obj, second)
                    }
                    is Div -> when (second) {
                        is Byte -> obj / second
                        is Short -> obj / second
                        is Int -> obj / second
                        is Long -> obj / second
                        is Float -> obj / second
                        is Double -> obj / second
                        else -> super.execute(op, obj, second)
                    }
                    is Rem -> when (second) {
                        is Byte -> obj % second
                        is Short -> obj % second
                        is Int -> obj % second
                        is Long -> obj % second
                        is Float -> obj % second
                        is Double -> obj % second
                        else -> super.execute(op, obj, second)
                    }
                    is Shl ->
                        if (second is Number) (obj.toLong() shl second.toInt()).toDouble()
                        else super.execute(op, obj, second)
                    is Shr ->
                        if (second is Number) (obj.toLong() shr second.toInt()).toDouble()
                        else super.execute(op, obj, second)
                    is Ushr ->
                        if (second is Number) (obj.toLong() ushr second.toInt()).toDouble()
                        else super.execute(op, obj, second)
                    is Or ->
                        if (second is Number) (obj.toLong() or second.toLong()).toDouble()
                        else super.execute(op, obj, second)
                    is And ->
                        if (second is Number) (obj.toLong() and second.toLong()).toDouble()
                        else super.execute(op, obj, second)
                    else -> super.execute(op, obj, second)
                }
            }
        })
        register(Boolean::class.javaObjectType, "base", "bool")
        register(object : PalmType(TypeName("base", "char"), Char::class.javaObjectType) {
            init {
                populate()
            }

            override fun execute(op: BinaryOperation, obj: Any?, second: Any?): Any {
                obj as Char
                return when (op) {
                    is Plus -> when (second) {
                        is Byte -> obj + second.toInt()
                        is Short -> obj + second.toInt()
                        is Int -> obj + second
                        else -> super.execute(op, obj, second)
                    }
                    is Minus -> when (second) {
                        is Byte -> obj - second.toInt()
                        is Short -> obj - second.toInt()
                        is Int -> obj - second
                        is Char -> obj - second
                        else -> super.execute(op, obj, second)
                    }
                    else -> super.execute(op, obj, second)
                }
            }

            override fun execute(op: MultiOperation, obj: Any?, rest: List<Any?>): Any {
                obj as Char
                val second = rest.first()
                return if (op == RangeTo && second is Char) when (val third = rest.getOrNull(1)) {
                    null -> if (second >= obj) obj..second else obj downTo second
                    is Char -> CharProgression.fromClosedRange(obj, third, second - obj)
                    else -> super.execute(op, obj, rest)
                } else super.execute(op, obj, rest)
            }
        })
        register(object : PalmType(TypeName("base", "string"), String::class.java) {
            init {
                populate()
                multiOps[Get] = MethodHandles.publicLookup().findVirtual(
                    String::class.java, "charAt",
                    MethodType.methodType(Char::class.java, Int::class.java)
                )
            }

            override fun iterator(obj: Any?) = (obj as String).iterator()

            override fun execute(op: BinaryOperation, obj: Any?, second: Any?) = if (op == Plus)
                (obj as String) + second else super.execute(op, obj, second)

            override fun cast(obj: Any?, type: Class<*>) =
                if (type.isEnum) MethodHandles.publicLookup()
                    .findStatic(type, "valueOf", MethodType.methodType(type, String::class.java))
                    .invokeWithArguments(obj)
                else super.cast(obj, type)
        })
        getOrRegister<Iterator<*>>()
        getOrRegister<Iterable<*>>()
        getOrRegister<Collection<*>>()
        getOrRegister<AbstractCollection<*>>().invoke {
            val loopUp = MethodHandles.publicLookup()
            autoCasters[LinkedList::class.java] =
                loopUp.findConstructor(
                    LinkedList::class.java,
                    MethodType.methodType(Void::class.javaPrimitiveType!!, Collection::class.java)
                )
            autoCasters[ArrayList::class.java] =
                loopUp.findConstructor(
                    ArrayList::class.java,
                    MethodType.methodType(Void::class.javaPrimitiveType!!, Collection::class.java)
                )
            autoCasters[Vector::class.java] =
                loopUp.findConstructor(
                    Vector::class.java,
                    MethodType.methodType(Void::class.javaPrimitiveType!!, Collection::class.java)
                )
            autoCasters[PriorityQueue::class.java] =
                loopUp.findConstructor(
                    PriorityQueue::class.java,
                    MethodType.methodType(Void::class.javaPrimitiveType!!, Collection::class.java)
                )
            autoCasters[HashSet::class.java] =
                loopUp.findConstructor(
                    HashSet::class.java,
                    MethodType.methodType(Void::class.javaPrimitiveType!!, Collection::class.java)
                )
            autoCasters[LinkedHashSet::class.java] =
                loopUp.findConstructor(
                    LinkedHashSet::class.java,
                    MethodType.methodType(Void::class.javaPrimitiveType!!, Collection::class.java)
                )
            autoCasters[TreeSet::class.java] =
                loopUp.findConstructor(
                    TreeSet::class.java,
                    MethodType.methodType(Void::class.javaPrimitiveType!!, Collection::class.java)
                )
        }
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
        getOrRegister<Pair<*, *>>()
        getOrRegister<IntRange>()
        getOrRegister<CharRange>()
        getOrRegister<LongRange>()
        getOrRegister<Random>()
    }

    fun register(type: IPalmType) {
        NAMES[type.name.name, type.name.path] = type.clazz
        TYPES[type.clazz] = type
    }

    inline fun <reified T> getOrRegister() = getOrRegister(T::class.java)

    fun getOrRegister(clazz: Class<*>) = register(
        clazz, (clazz.getAnnotation(Palm.Name::class.java)?.name ?: clazz.typeName.toSnakeCase()).toTypeName()
    )

    fun register(clazz: Class<*>, path: String, name: String = clazz.simpleName) =
        register(clazz, TypeName(path, name))

    fun register(clazz: Class<*>, name: TypeName): IPalmType {
        TYPES[clazz]?.let { return it }
        val type = PalmType(name, clazz)
        register(type)
        if (!clazz.isInterface) type.populate()
        return type
    }

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
                    type.multiOps[RangeTo] =
                        loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                registryName.startsWith("to") && registryName.length > 2 && it.parameterCount == 1 && it.returnType != extending && it.returnType != Void::class.javaPrimitiveType -> {
                    type.autoCasters[it.returnType] =
                        loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, extending))
                }
                registryName in UNARY_OPS -> {
                    val op = UNARY_OPS[registryName]
                    if (it.parameterCount == 1)
                        type.unaryOps[op!!] =
                            loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, extending))
                }
                it.parameterCount == 1 && it.returnType != Void::class.javaPrimitiveType -> {
                    val getterName = (if (registryName.startsWith("get")) registryName.drop(3)
                        .decapitalize() else registryName).toSnakeCase()
                    val method = loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, extending))
                    if (registryName == "iter" || registryName == "iterator") type.iterator = method
                    type.getters[getterName] = method
                }
                it.parameterCount == 2 && it.returnType == Void::class.javaPrimitiveType -> {
                    val setterName = (if (registryName.startsWith("set")) registryName.drop(3)
                        .decapitalize() else registryName).toSnakeCase()
                    type.setters[setterName] =
                        loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
                else -> BINARY_OPS[registryName]?.let { op ->
                    if (it.parameterCount == 2 && op.returnType?.equals(it.returnType) != false)
                        type.binaryOps[op] =
                            loopUp.findStatic(clazz, it.name, MethodType.methodType(it.returnType, it.parameterTypes))
                }
            }
        }
    }
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