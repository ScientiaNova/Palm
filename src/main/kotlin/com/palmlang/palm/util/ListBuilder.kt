package com.palmlang.palm.util

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