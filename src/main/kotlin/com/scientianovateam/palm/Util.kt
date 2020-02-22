package com.scientianovateam.palm

import java.util.*

fun <T> Stack<T>.safePop() = if (isEmpty()) null else pop()
fun <T> Stack<T>.safePeek() = if (isEmpty()) null else peek()