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
object VirtualToken : IdentifierToken("virtual")


private val specialWords = mapOf(
    "_" to WildcardToken,
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
    "where" to WhereToken,
    "as" to AsToken
)

fun handleUncapitalizedString(string: String) = specialWords[string] ?: IdentifierToken(string)