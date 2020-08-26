package com.scientianova.palm.util

data class Cons<out T>(val value: T, val next: Cons<T>?)

fun <T> Array<out T>.toCons() = buildCons(this, lastIndex, null)

private tailrec fun <T> buildCons(array: Array<T>, index: Int, last: Cons<T>?): Cons<T>? =
    if (index < 0) last else buildCons(array, index - 1, Cons(array[index], last))