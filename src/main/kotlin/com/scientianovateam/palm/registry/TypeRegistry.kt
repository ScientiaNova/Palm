package com.scientianovateam.palm.registry

import com.scientianovateam.palm.util.HashTable

object TypeRegistry {
    private val nameToClass = HashTable<String, String, Class<*>>()
    fun classFromName(name: String, path: String? = null) =
        if (path.isNullOrBlank()) nameToClass[name]?.values?.firstOrNull() else nameToClass[name, path]

    private val palmTypes = hashMapOf<Class<*>, PalmType>()
    fun getPalmType(clazz: Class<*>) = palmTypes[clazz]
    fun getPalmType(name: String, path: String? = null) = classFromName(name, path)?.let { getPalmType(it) }
}