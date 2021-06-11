package com.palmlang.palm.util

sealed class Either<out L, out R>
data class Right<R>(val right: R) : Either<Nothing, R>()
data class Left<L>(val left: L) : Either<L, Nothing>()