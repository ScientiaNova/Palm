package com.scientianova.palm.parser

class CharGroup(private val predicate: Char.() -> Boolean) {
    operator fun contains(char: Char) = predicate(char)
    operator fun contains(char: Char?) = char != null && predicate(char)
}

val symbolChars = CharGroup(Char::isSymbolPart)
val identStartChars = CharGroup { isLetter() || equals('_') }
val identChars = CharGroup { isLetterOrDigit() || equals('_') }
val separatorChars = CharGroup(Char::isSeparator)