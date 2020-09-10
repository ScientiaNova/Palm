package com.scientianova.palm.util

fun <A, B, C> Pair<A, B>.mapFirst(fn: (A) -> C) = fn(first) to second
fun <A, B, C> Pair<A, B>.mapSecond(fn: (B) -> C) = first to fn(second)