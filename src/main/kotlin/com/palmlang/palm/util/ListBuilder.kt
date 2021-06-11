package com.palmlang.palm.util

fun <T> MutableList<T>.alsoAdd(elem: T) = also { add(elem) }

inline fun <T> recBuildList(
    list: MutableList<T> = mutableListOf(),
    builder: MutableList<T>.() -> Unit
): List<T> {
    while (true) builder(list)
}

inline fun <T> recBuildListN(
    list: MutableList<T> = mutableListOf(),
    builder: MutableList<T>.() -> Unit
): Nothing {
    while (true) builder(list)
}