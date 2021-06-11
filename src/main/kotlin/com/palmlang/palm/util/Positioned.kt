package com.palmlang.palm.util

typealias StringPos = Int

data class Positioned<out T>(val value: T, val start: StringPos, val next: StringPos) {
    override fun toString() = value.toString()
}

val Positioned<*>.end get() = next - 1

fun <T> T.at(start: StringPos, next: StringPos = start + 1) = Positioned(this, start, next)

inline fun <A, B> Positioned<A>.map(fn: (A) -> B) = Positioned(fn(value), start, next)

typealias PString = Positioned<String>