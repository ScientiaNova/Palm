package com.scientianova.palm.parser

data class TopLevelPropertyInfo(
    val privacy: TopLevelPrivacy,
    val inline: Boolean,
    val lateInit: Boolean,
    val given: Boolean,
    val using: Boolean
)

data class ClassPropertyInfo(
    val privacy: ClassLevelPrivacy,
    val abstract: Boolean,
    val lateInit: Boolean,
    val given: Boolean,
    val using: Boolean
)