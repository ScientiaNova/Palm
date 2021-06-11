package com.palmlang.palm.util

class MultiHashMap<K, V>(private val backing: MutableMap<K, MutableList<V>> = mutableMapOf()) : Map<K, List<V>> by backing {
    fun add(key: K, value: V) {
        backing.computeIfAbsent(key) { mutableListOf() }.add(value)
    }

    fun addAll(key: K, values: Collection<V>) {
        backing.computeIfAbsent(key) { mutableListOf() }.addAll(values)
    }

    fun remove(key: K) {
        backing.remove(key)
    }
}