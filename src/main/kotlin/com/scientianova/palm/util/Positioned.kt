package com.scientianova.palm.util

data class Positioned<out T>(val value: T, val area: StringArea) {
    override fun toString() = value.toString()
}

infix fun <T> T.on(other: Positioned<*>) =
    Positioned(this, other.area)

infix fun <T> T.on(area: StringArea) = Positioned(this, area)
infix fun <T> T.on(pos: StringPos) = Positioned(this, pos..pos)

typealias PString = Positioned<String>