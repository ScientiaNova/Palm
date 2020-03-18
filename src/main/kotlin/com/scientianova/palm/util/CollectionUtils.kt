package com.scientianova.palm.util

import java.util.*

fun <T> Queue<T>.offering(thing: T) = this.also { offer(thing) }
fun <T> Stack<T>.pushing(thing: T) = this.also { push(thing) }