package com.scientianova.palm.util

typealias StringPos = Int
typealias StringArea = IntRange

data class Positioned<out T>(val value: T, val start: StringPos, val next: StringPos) {
    override fun toString() = value.toString()
}

fun <T> T.at(start: StringPos, end: StringPos = start) = Positioned(this, start, end)

typealias PInt = Positioned<Int>
typealias PString = Positioned<String>