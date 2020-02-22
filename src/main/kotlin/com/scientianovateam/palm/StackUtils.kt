package com.scientianovateam.palm

import java.util.*

fun <T> Stack<T>.safePop() = if (isEmpty()) null else pop()
fun <T> Stack<T>.safePeek() = if (isEmpty()) null else peek()
fun <T> Stack<T>.flip() = Stack<T>().apply {
    while (this@flip.isNotEmpty())
        push(this@flip.pop())
}