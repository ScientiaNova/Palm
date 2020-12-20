package com.scientianova.palm.util

sealed class ListBuilder<out T : Any> {
    abstract val index: Int
    internal abstract val parent: ListBuilder<T>
    fun buildList(): List<T> {
        @Suppress("UNCHECKED_CAST")
        val array = arrayOfNulls<Any>(index + 1) as Array<T>
        var curr = this
        while (curr !== Null) {
            curr.buildArray(array)
            curr = curr.parent
        }
        return ArrayList(array)
    }


    private class ArrayList<T>(val array: Array<T>) : AbstractList<T>() {
        override val size: Int
            get() = array.size

        override fun contains(element: T) = element in array
        override fun get(index: Int) = array[index]
        override fun indexOf(element: T) = array.indexOf(element)
        override fun isEmpty() = array.isEmpty()
        override fun iterator() = array.iterator()
        override fun lastIndexOf(element: T) = array.lastIndexOf(element)
    }

    object Null : ListBuilder<Nothing>() {
        override val index get() = -1
        override val parent get() = this
    }

    data class ConsBranch<T : Any>(
        val branch: ListBuilder<T>,
        override val parent: ListBuilder<T>
    ) : ListBuilder<T>() {
        override val index get() = parent.index + parent.index + 1
    }

    data class ConsSingle<T : Any>(
        val elem: T,
        override val index: Int,
        override val parent: ListBuilder<T>
    ) : ListBuilder<T>()
}

fun <T: Any> ListBuilder<T>.buildArray(array: Array<in T>) {
    when (this) {
        is ListBuilder.ConsSingle -> {
            array[index] = elem
        }
        is ListBuilder.ConsBranch -> {
            val offset = parent.index + 1
            branch.buildArray(array, offset)
        }
        else -> Unit
    }
}

fun <T: Any> ListBuilder<T>.buildArray(array: Array<in T>, offset: Int) {
    when (this) {
        is ListBuilder.ConsSingle -> {
            array[index + offset] = elem
        }
        is ListBuilder.ConsBranch -> {
            val offset1 = parent.index + 1
            branch.buildArray(array, offset + offset1)
        }
        else -> Unit
    }
}

operator fun <T : Any> ListBuilder<T>.plus(elem: T) = ListBuilder.ConsSingle(elem, index + 1, this)

inline fun <T : Any> recBuildList(
    list: ListBuilder<T> = ListBuilder.Null,
    builder: ListBuilder<T>.() -> ListBuilder<T>
): List<T> {
    while (true) builder(list)
}

inline fun <T : Any> recBuildListN(
    list: ListBuilder<T> = ListBuilder.Null,
    builder: ListBuilder<T>.() -> ListBuilder<T>
): Nothing {
    while (true) builder(list)
}