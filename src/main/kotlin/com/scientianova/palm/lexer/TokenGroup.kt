package com.scientianova.palm.lexer

data class TokenGroup(val predicate: (Token) -> Boolean) {
    operator fun contains(token: Token) = predicate(token)
}

val identTokens = TokenGroup { it.identString().isNotEmpty() }
val prefixTokens = TokenGroup(Token::isPrefix)
val postfixTokens = TokenGroup(Token::isPostfix)