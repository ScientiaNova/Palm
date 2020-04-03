package com.scientianova.palm.registry

import com.scientianova.palm.evaluator.Scope
import com.scientianova.palm.evaluator.palm
import com.scientianova.palm.tokenizer.StringTraverser
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Modifier
import java.util.*

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
        })
        register(Byte::class.javaObjectType, "base", "byte")
        register(Short::class.javaObjectType, "base", "short")
        register(Int::class.javaObjectType, "base", "int")
        register(Long::class.javaObjectType, "base", "long")
        register(Float::class.javaObjectType, "base", "float")
        register(Double::class.javaObjectType, "base", "double")
        register(Boolean::class.javaObjectType, "base", "bool")
        register(object : PalmType(listOf("base", "string"), String::class.java) {
            init {
                populate()
                virtual["get"] = MethodHandles.publicLookup().findVirtual(
                    String::class.java, "charAt",
                    MethodType.methodType(Char::class.java, Int::class.java)
                )
            }
        })
        getOrRegister(Char::class.javaObjectType)
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
        PalmType(
            listOf("base", "infix_extensions"),
            Class.forName("com.scientianova.palm.registry.InfixCopiesKt")
        ).let {
            it.populate()
            register(it)
        }
    }

    fun register(type: IPalmType): IPalmType {
        TYPES[type.clazz]?.let { return it }
        RootPathNode.addType(type.name, type).also { node ->
            if (type.name.first() == "base" && type.clazz.enclosingClass == null) {
                val simpleName = type.name.last()
                if (type.constructors.isNotEmpty())
                    Scope.GLOBAL.imports[simpleName] = node
                type.constructors.forEach {
                    Scope.GLOBAL.staticImports[simpleName] = it
                }
                for ((name, functions) in type.static)
                    functions.forEach { Scope.GLOBAL.addStaticImport(name, it) }
            }
        }
        TYPES[type.clazz] = type
        for (nested in type.clazz.declaredClasses) if (Modifier.isPublic(nested.modifiers)) getOrRegister(nested)
        return type
    }

    inline fun <reified T> getOrRegister() = getOrRegister(T::class.java)

    fun getOrRegister(clazz: Class<*>) = TYPES[clazz] ?: register(
        clazz, clazz.getAnnotation(Palm.Name::class.java)?.name?.split('.')
            ?: clazz.enclosingClass?.let { enclosing -> enclosing.palm.name + clazz.simpleName.toSnakeCase() }
            ?: clazz.typeName.toList()
    )

    fun register(clazz: Class<*>, vararg path: String) = register(clazz, path.toList())

    fun register(clazz: Class<*>, path: List<String>): IPalmType {
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