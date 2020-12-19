package com.scientianova.palm.util

sealed class ListBuilder<T> {
    abstract val index: Int
    abstract operator fun plus(elem: T): ListBuilder<T>
    fun buildList(): List<T> {
        @Suppress("UNCHECKED_CAST")
        val array = arrayOfNulls<Any>(index + 1) as Array<T>
        buildArray(array)
        return listOf(*array)
    }

    internal abstract fun buildArray(array: Array<T>)

    class Null<T> : ListBuilder<T>() {
        override val index get() = -1
        override fun plus(elem: T): ListBuilder<T> = Cons(elem, 0, this)
        override fun buildArray(array: Array<T>) = Unit
    }

    data class Cons<T>(
        private val curr: T,
        override val index: Int,
        private val parent: ListBuilder<T>
        ) : ListBuilder<T>() {
        override fun plus(elem: T) = Cons(elem, index + 1, this)
        override fun buildArray(array: Array<T>) {
            array[index] = curr
            parent.buildArray(array)
        }
    }
}

inline fun <T> recBuildList(list: ListBuilder<T> = ListBuilder.Null(), builder: ListBuilder<T>.() -> ListBuilder<T>): List<T> {
    while (true) builder(list)
}

inline fun <T> recBuildListN(list: ListBuilder<T> = ListBuilder.Null(), builder: ListBuilder<T>.() -> ListBuilder<T>): Nothing {
    while (true) builder(list)
}