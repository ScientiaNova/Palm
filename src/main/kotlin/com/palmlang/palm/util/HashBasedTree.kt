package com.palmlang.palm.util

data class HashBasedTree<K, V>(var value: V? = null, val children: MutableMap<K, HashBasedTree<K, V>> = mutableMapOf())