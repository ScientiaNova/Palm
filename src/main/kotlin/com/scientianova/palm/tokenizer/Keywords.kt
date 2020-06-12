package com.scientianova.palm.tokenizer

private val specialWords = mapOf(
    "_" to WildcardToken,
    "when" to WhenToken,
    "import" to ImportToken,
    "let" to LetToken
)

fun checkKeywords(string: String, capitalized: Boolean) = specialWords[string] ?: IdentifierToken(string, capitalized)