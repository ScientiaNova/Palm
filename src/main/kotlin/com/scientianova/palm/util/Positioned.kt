package com.scientianova.palm.util

typealias StringPos = Int
typealias StringArea = IntRange

data class Positioned<out T>(val value: T, val pos: StringPos) {
    override fun toString() = value.toString()
}

infix fun <T> T.at(pos: StringPos) = Positioned(this, pos)

typealias PString = Positioned<String>