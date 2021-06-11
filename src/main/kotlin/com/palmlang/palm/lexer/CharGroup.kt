package com.palmlang.palm.lexer

class CharGroup(private val predicate: Char.() -> Boolean) {
    operator fun contains(char: Char) = predicate(char)
    operator fun contains(char: Char?) = char != null && predicate(char)
}

val identStartChars = CharGroup(Char::isIdentifierStart)
val identChars = CharGroup(Char::isIdentifierPart)
val symbolChars = CharGroup {
    when (this) {
        '+', '-', '%', '=', '>', '<', '!', '&', '|', '?', ':' -> true
        else -> false
    }
}