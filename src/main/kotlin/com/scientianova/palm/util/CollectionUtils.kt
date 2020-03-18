package com.scientianova.palm.util

import java.util.*

fun <T> Stack<T>.pushing(thing: T) = this.also { push(thing) }