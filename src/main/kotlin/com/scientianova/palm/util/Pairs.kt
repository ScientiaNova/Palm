package com.scientianova.palm.util

inline fun <A, B, C> Pair<A, B>.mapFirst(fn: (A) -> C): Pair<C, B> = fn(first) to second
inline fun <A, B, C> Pair<A, B>.mapSecond(fn: (B) -> C): Pair<A, C> = first to fn(second)