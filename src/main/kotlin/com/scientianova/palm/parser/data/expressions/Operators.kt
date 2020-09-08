package com.scientianova.palm.parser.data.expressions

enum class BinaryOp {
    Plus, Minus, Times, Div, Rem, RangeTo, LT, GT, LTEq, GTEq
}

enum class UnaryOp {
    Plus, Minus, Not, NonNull, RangeFrom, RangeUntil
}

fun UnaryOp.isPrefix() = when (this) {
    UnaryOp.NonNull, UnaryOp.RangeFrom -> false
    else -> true
}

enum class AssignmentType {
    Normal, Plus, Minus, Times, Div, Rem
}