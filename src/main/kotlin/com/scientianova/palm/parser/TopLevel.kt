package com.scientianova.palm.parser

enum class DecProperty {
    Using,
    Given,
    Inline,
    Leaf,
    Abstract,
    Public,
    Private,
    Protected,
    Internal,
    Const,
    LateInit,
    TailRec,
    Inner
}

enum class TopLevelPrivacy {
    Public, Internal, Private
}