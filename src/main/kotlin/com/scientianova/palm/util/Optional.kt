package com.scientianova.palm.util

sealed class Optional<out T>
data class Some<T>(val value: T) : Optional<T>()
object None : Optional<Nothing>()