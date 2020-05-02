package com.scientianova.palm.tokenizer

sealed class KeywordToken(private val word: String) : IToken {
    override fun toString() = "KeywordToken(word=$word)"
}

object WildcardToken : IToken {
    override fun toString() = "WildcardToken"
}

object IfToken : KeywordToken("if")
object WhenToken : KeywordToken("when")
object ForToken : KeywordToken("for")
object GuardToken : KeywordToken("guard")
object ImportToken : KeywordToken("import")
object ReturnToken : KeywordToken("return")
object BreakToken : KeywordToken("break")
object ContinueToken : KeywordToken("continue")
object TryToken : KeywordToken("try")
object FallthroughToken : KeywordToken("fallthrough")
object LetToken : KeywordToken("let")
object LoopToken : KeywordToken("loop")
object ThisToken : KeywordToken("this")
object ThisTypeToken : KeywordToken("This")
object SuperToken : KeywordToken("super")
object WhileToken : KeywordToken("while")
object ThrowToken : KeywordToken("throw")
object FnToken : KeywordToken("fn")

sealed class BoolToken(val bool: Boolean) : IToken {
    override fun toString() = "BoolToken(value=$bool)"
}

object TrueToken : BoolToken(true)
object FalseToken : BoolToken(false)

object NullToken : IToken {
    override fun toString() = "NullToken"
}

sealed class ContainingOperatorToken(symbol: String) : InfixOperatorToken(symbol)
object InToken : ContainingOperatorToken("in"), TypeVariableModifier

sealed class TypeOperatorToken(symbol: String) : InfixOperatorToken(symbol)
object IsToken : TypeOperatorToken("is")

object AsToken : TypeOperatorToken("as")

private val specialWords = mapOf(
    "_" to WildcardToken,
    "true" to TrueToken,
    "false" to FalseToken,
    "null" to NullToken,
    "if" to IfToken,
    "else" to ElseToken,
    "when" to WhenToken,
    "for" to ForToken,
    "guard" to GuardToken,
    "in" to InToken,
    "is" to IsToken,
    "as" to AsToken,
    "import" to ImportToken,
    "return" to ReturnToken,
    "continue" to ContinueToken,
    "break" to BreakToken,
    "try" to TryToken,
    "catch" to CatchToken,
    "finally" to FinallyToken,
    "nobreak" to NobreakToken,
    "fallthrought" to FallthroughToken,
    "let" to LetToken,
    "loop" to LoopToken,
    "this" to ThisToken,
    "This" to ThisTypeToken,
    "super" to SuperToken,
    "while" to WhileToken,
    "throw" to ThrowToken,
    "fn" to FnToken,
    "get" to GetToken,
    "set" to SetToken,
    "constructor" to ConstructorToken,
    "init" to InitToken,
    "type" to TypeToken,
    "class" to ClassToken,
    "alias" to AliasToken,
    "interface" to InterfaceToken,
    "object" to ObjectToken,
    "enum" to EnumToken,
    "record" to RecordToken,
    "annotation" to AnnotationToken,
    "operator" to OperatorToken,
    "out" to OutToken,
    "mut" to MutToken,
    "override" to OverrideToken,
    "abstract" to AbstractToken,
    "lateinit" to LateinitToken,
    "private" to PrivateToken,
    "protected" to ProtectedToken,
    "internal" to InternalToken,
    "public" to PublicToken,
    "open" to OpenToken,
    "final" to FinalToken,
    "inline" to InlineToken,
    "tailrec" to TailrecToken,
    "const" to ConstToken,
    "companion" to CompanionToken,
    "inner" to InnerToken,
    "sealed" to SealedToken,
    "autofn" to AutofnToken,
    "noinline" to NoinlineToken,
    "prefix" to PrefixToken,
    "infix" to InfixToken,
    "postfix" to PostfixToken,
    "impl" to ImplToken,
    "where" to WhereToken
)

fun handleUncapitalizedString(string: String) = specialWords[string] ?: IdentifierToken(string)