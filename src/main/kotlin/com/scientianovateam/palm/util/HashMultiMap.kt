package com.scientianovateam.palm.util

class HashMultiMap<K, V> : HashMap<K, MutableList<V>>() {
    fun put(key: K, value: V) {
        val list = get(key) ?: mutableListOf<V>().also { put(key, it) }
        list.add(value)
    }

    fun remove(key: K, value: V) {
        val list = get(key) ?: return
        list.remove(value)
        if (list.isEmpty())
            remove(key)
    }

    operator fun set(key: K, value: V) = put(key, value)
}