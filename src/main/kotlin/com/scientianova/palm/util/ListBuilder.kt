package com.scientianova.palm.util

fun <T> MutableList<T>.alsoAdd(elem: T) = also { add(elem) }

inline fun <T : Any> recBuildList(
    list: MutableList<T> = mutableListOf(),
    builder: MutableList<T>.() -> Unit
): List<T> {
    while (true) builder(list)
}

inline fun <T : Any> recBuildListN(
    list: MutableList<T> = mutableListOf(),
    builder: MutableList<T>.() -> Unit
): Nothing {
    while (true) builder(list)
}