package com.scientianovateam.palm.util

class MultiHashMap<K, V> : HashMap<K, MutableList<V>>() {
    operator fun set(key: K, value: V) {
        val list = get(key) ?: mutableListOf<V>().also { put(key, it) }
        list.add(value)
    }
}