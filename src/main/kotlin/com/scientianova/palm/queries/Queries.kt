package com.scientianova.palm.queries

data class InputQueryValue<T>(val value: T, val changed: Revision)
data class ComputeQueryValue<T>(val value: T, val changed: Revision, val checked: Revision)

class InputQuery<K, V> {
    private val map = hashMapOf<K, InputQueryValue<V>>()
    operator fun set(key: K, value: V) {

    }
}