package com.scientianova.palm.parser.expressions

enum class BinaryOp {
    Plus, Minus, Times, Div, Rem, RangeTo, LT, GT, LTEq, GTEq
}

enum class UnaryOp {
    Plus, Minus, Not, NonNull, RangeFrom, RangeUntil
}

enum class AssignmentType {
    Normal, Plus, Minus, Times, Div, Rem
}