package com.scientianova.palm.tokenizer

sealed class KeywordToken(private val word: String) : IToken {
    override fun toString() = "KeywordToken(word=$word)"
}

object WildcardToken : IToken {
    override fun toString() = "WildcardToken"
}

object IfToken : KeywordToken("if")
object WhenToken : KeywordToken("when")
object ImportToken : KeywordToken("import")
object LetToken : KeywordToken("let")

sealed class BoolToken(val bool: Boolean) : IToken {
    override fun toString() = "BoolToken(value=$bool)"
}

object TrueToken : BoolToken(true)
object FalseToken : BoolToken(false)

private val specialWords = mapOf(
    "_" to WildcardToken,
    "true" to TrueToken,
    "false" to FalseToken,
    "if" to IfToken,
    "else" to ElseToken,
    "when" to WhenToken,
    "import" to ImportToken,
    "let" to LetToken,
    "virtual" to VirtualToken,
    "class" to ClassToken,
    "alias" to AliasToken,
    "object" to ObjectToken,
    "enum" to EnumToken,
    "record" to RecordToken,
    "operator" to OperatorToken,
    "mut" to MutToken,
    "impl" to ImplToken,
    "where" to WhereToken
)

fun handleUncapitalizedString(string: String) = specialWords[string] ?: IdentifierToken(string)