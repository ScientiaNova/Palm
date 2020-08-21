package com.scientianova.palm.parser

enum class ClassLevelPrivacy {
    Public, Protected, Private
}

enum class ClassImplementation {
    Leaf, Full, Abstract
}

data class ClassInfo(
    val privacy: TopLevelPrivacy,
    val implementation: ClassImplementation
)