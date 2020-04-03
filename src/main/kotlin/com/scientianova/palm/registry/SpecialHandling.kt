package com.scientianova.palm.registry

import kotlin.experimental.inv

fun handlePrimitives(name: String, first: Any?, rest: List<Any?>): Any? = when {
    first is Number -> when (rest.size) {
        0 -> when (name) {
            "unary_minus" -> when (first) {
                is Byte -> -first
                is Short -> -first
                is Int -> -first
                is Long -> -first
                is Float -> -first
                is Double -> -first
                else -> null
            }
            "unary_plus" -> when (first) {
                is Byte -> first
                is Short -> first
                is Int -> first
                is Long -> first
                is Float -> first
                is Double -> first
                else -> null
            }
            "inv" -> when (first) {
                is Byte -> first.inv()
                is Short -> first.inv()
                is Int -> first.inv()
                is Long -> first.inv()
                else -> null
            }
            else -> null
        }
        1 -> {
            val second = rest[0]
            if (second is Number) when (name) {
                "plus" -> when (first) {
                    is Byte -> when (second) {
                        is Byte -> first + second
                        is Short -> first + second
                        is Int -> first + second
                        is Long -> first + second
                        is Float -> first + second
                        is Double -> first + second
                        else -> null
                    }
                    is Short -> when (second) {
                        is Byte -> first + second
                        is Short -> first + second
                        is Int -> first + second
                        is Long -> first + second
                        is Float -> first + second
                        is Double -> first + second
                        else -> null
                    }
                    is Int -> when (second) {
                        is Byte -> first + second
                        is Short -> first + second
                        is Int -> first + second
                        is Long -> first + second
                        is Float -> first + second
                        is Double -> first + second
                        else -> null
                    }
                    is Long -> when (second) {
                        is Byte -> first + second
                        is Short -> first + second
                        is Int -> first + second
                        is Long -> first + second
                        is Float -> first + second
                        is Double -> first + second
                        else -> null
                    }
                    is Float -> when (second) {
                        is Byte -> first + second
                        is Short -> first + second
                        is Int -> first + second
                        is Long -> first + second
                        is Float -> first + second
                        is Double -> first + second
                        else -> null
                    }
                    is Double -> when (second) {
                        is Byte -> first + second
                        is Short -> first + second
                        is Int -> first + second
                        is Long -> first + second
                        is Float -> first + second
                        is Double -> first + second
                        else -> null
                    }
                    else -> null
                }
                "minus" -> when (first) {
                    is Byte -> when (second) {
                        is Byte -> first - second
                        is Short -> first - second
                        is Int -> first - second
                        is Long -> first - second
                        is Float -> first - second
                        is Double -> first - second
                        else -> null
                    }
                    is Short -> when (second) {
                        is Byte -> first - second
                        is Short -> first - second
                        is Int -> first - second
                        is Long -> first - second
                        is Float -> first - second
                        is Double -> first - second
                        else -> null
                    }
                    is Int -> when (second) {
                        is Byte -> first - second
                        is Short -> first - second
                        is Int -> first - second
                        is Long -> first - second
                        is Float -> first - second
                        is Double -> first - second
                        else -> null
                    }
                    is Long -> when (second) {
                        is Byte -> first - second
                        is Short -> first - second
                        is Int -> first - second
                        is Long -> first - second
                        is Float -> first - second
                        is Double -> first - second
                        else -> null
                    }
                    is Float -> when (second) {
                        is Byte -> first - second
                        is Short -> first - second
                        is Int -> first - second
                        is Long -> first - second
                        is Float -> first - second
                        is Double -> first - second
                        else -> null
                    }
                    is Double -> when (second) {
                        is Byte -> first - second
                        is Short -> first - second
                        is Int -> first - second
                        is Long -> first - second
                        is Float -> first - second
                        is Double -> first - second
                        else -> null
                    }
                    else -> null
                }
                "mul" -> when (first) {
                    is Byte -> when (second) {
                        is Byte -> first * second
                        is Short -> first * second
                        is Int -> first * second
                        is Long -> first * second
                        is Float -> first * second
                        is Double -> first * second
                        else -> null
                    }
                    is Short -> when (second) {
                        is Byte -> first * second
                        is Short -> first * second
                        is Int -> first * second
                        is Long -> first * second
                        is Float -> first * second
                        is Double -> first * second
                        else -> null
                    }
                    is Int -> when (second) {
                        is Byte -> first * second
                        is Short -> first * second
                        is Int -> first * second
                        is Long -> first * second
                        is Float -> first * second
                        is Double -> first * second
                        else -> null
                    }
                    is Long -> when (second) {
                        is Byte -> first * second
                        is Short -> first * second
                        is Int -> first * second
                        is Long -> first * second
                        is Float -> first * second
                        is Double -> first * second
                        else -> null
                    }
                    is Float -> when (second) {
                        is Byte -> first * second
                        is Short -> first * second
                        is Int -> first * second
                        is Long -> first * second
                        is Float -> first * second
                        is Double -> first * second
                        else -> null
                    }
                    is Double -> when (second) {
                        is Byte -> first * second
                        is Short -> first * second
                        is Int -> first * second
                        is Long -> first * second
                        is Float -> first * second
                        is Double -> first * second
                        else -> null
                    }
                    else -> null
                }
                "div" -> when (first) {
                    is Byte -> when (second) {
                        is Byte -> first / second
                        is Short -> first / second
                        is Int -> first / second
                        is Long -> first / second
                        is Float -> first / second
                        is Double -> first / second
                        else -> null
                    }
                    is Short -> when (second) {
                        is Byte -> first / second
                        is Short -> first / second
                        is Int -> first / second
                        is Long -> first / second
                        is Float -> first / second
                        is Double -> first / second
                        else -> null
                    }
                    is Int -> when (second) {
                        is Byte -> first / second
                        is Short -> first / second
                        is Int -> first / second
                        is Long -> first / second
                        is Float -> first / second
                        is Double -> first / second
                        else -> null
                    }
                    is Long -> when (second) {
                        is Byte -> first / second
                        is Short -> first / second
                        is Int -> first / second
                        is Long -> first / second
                        is Float -> first / second
                        is Double -> first / second
                        else -> null
                    }
                    is Float -> when (second) {
                        is Byte -> first / second
                        is Short -> first / second
                        is Int -> first / second
                        is Long -> first / second
                        is Float -> first / second
                        is Double -> first / second
                        else -> null
                    }
                    is Double -> when (second) {
                        is Byte -> first / second
                        is Short -> first / second
                        is Int -> first / second
                        is Long -> first / second
                        is Float -> first / second
                        is Double -> first / second
                        else -> null
                    }
                    else -> null
                }
                "rem" -> when (first) {
                    is Byte -> when (second) {
                        is Byte -> first % second
                        is Short -> first % second
                        is Int -> first % second
                        is Long -> first % second
                        is Float -> first % second
                        is Double -> first % second
                        else -> null
                    }
                    is Short -> when (second) {
                        is Byte -> first % second
                        is Short -> first % second
                        is Int -> first % second
                        is Long -> first % second
                        is Float -> first % second
                        is Double -> first % second
                        else -> null
                    }
                    is Int -> when (second) {
                        is Byte -> first % second
                        is Short -> first % second
                        is Int -> first % second
                        is Long -> first % second
                        is Float -> first % second
                        is Double -> first % second
                        else -> null
                    }
                    is Long -> when (second) {
                        is Byte -> first % second
                        is Short -> first % second
                        is Int -> first % second
                        is Long -> first % second
                        is Float -> first % second
                        is Double -> first % second
                        else -> null
                    }
                    is Float -> when (second) {
                        is Byte -> first % second
                        is Short -> first % second
                        is Int -> first % second
                        is Long -> first % second
                        is Float -> first % second
                        is Double -> first % second
                        else -> null
                    }
                    is Double -> when (second) {
                        is Byte -> first % second
                        is Short -> first % second
                        is Int -> first % second
                        is Long -> first % second
                        is Float -> first % second
                        is Double -> first % second
                        else -> null
                    }
                    else -> null
                }
                "range_to" -> when (first) {
                    is Byte -> when (second) {
                        is Byte -> first..second
                        is Short -> first..second
                        is Int -> first..second
                        is Long -> first..second
                        else -> null
                    }
                    is Short -> when (second) {
                        is Byte -> first..second
                        is Short -> first..second
                        is Int -> first..second
                        is Long -> first..second
                        else -> null
                    }
                    is Int -> when (second) {
                        is Byte -> first..second
                        is Short -> first..second
                        is Int -> first..second
                        is Long -> first..second
                        else -> null
                    }
                    is Long -> when (second) {
                        is Byte -> first..second
                        is Short -> first..second
                        is Int -> first..second
                        is Long -> first..second
                        else -> null
                    }
                    is Float ->
                        if (second is Float) first..second
                        else null
                    is Double ->
                        if (second is Double) first..second
                        else null
                    else -> null
                }
                "shl" -> when (first) {
                    is Int ->
                        if (second is Int) first shl second
                        else null
                    is Long ->
                        if (second is Int) first shl second
                        else null
                    else -> null
                }
                "shr" -> when (first) {
                    is Int ->
                        if (second is Int) first shr second
                        else null
                    is Long ->
                        if (second is Int) first shr second
                        else null
                    else -> null
                }
                "ushr" -> when (first) {
                    is Int ->
                        if (second is Int) first ushr second
                        else null
                    is Long ->
                        if (second is Int) first ushr second
                        else null
                    else -> null
                }
                "or" -> when (first) {
                    is Int ->
                        if (second is Int) first or second
                        else null
                    is Long ->
                        if (second is Long) first or second
                        else null
                    else -> null
                }
                "and" -> when (first) {
                    is Int ->
                        if (second is Int) first and second
                        else null
                    is Long ->
                        if (second is Long) first and second
                        else null
                    else -> null
                }
                else -> null
            }
            else null
        }
        else -> null
    }
    first is Char && rest.size == 1 -> {
        val second = rest[0]
        when (name) {
            "plus" -> when (second) {
                is Byte -> first + second.toInt()
                is Short -> first + second.toInt()
                is Int -> first + second
                else -> null
            }
            "minus" -> when (second) {
                is Byte -> first - second.toInt()
                is Short -> first - second.toInt()
                is Int -> first - second
                is Char -> first - second
                else -> null
            }
            "range_to" ->
                if (second is Char) first..second else null
            else -> null
        }
    }
    first is String && rest.size == 1 && name == "plus" -> first + rest[0]
    else -> null
}