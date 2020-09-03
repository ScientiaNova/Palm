package com.scientianova.palm.util

typealias StringPos = Int
typealias StringArea = IntRange

data class Positioned<out T>(val value: T, val area: StringArea) {
    override fun toString() = value.toString()
}

fun <T> T.at(start: StringPos, end: StringPos = start) = Positioned(this, start..end)

typealias PString = Positioned<String>