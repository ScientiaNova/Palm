package com.scientianova.palm.tokenizer

import com.scientianova.palm.parser.*

sealed class KeywordToken(private val word: String) : IToken {
    override fun toString() = "KeywordToken(word=$word)"
}

object IfToken : KeywordToken("if")
object WhenToken : KeywordToken("when")
object ForToken : KeywordToken("for")
object YieldToken : KeywordToken("yield")
object ImportToken : KeywordToken("import")
object ReturnToken : KeywordToken("return")
object BreakToken : KeywordToken("break")
object ContinueToken : KeywordToken("continue")
object TryToken : KeywordToken("try")
object FallthroughToken : KeywordToken("fallthrough")
object LetToken : KeywordToken("let")
object LoopToken : KeywordToken("loop")
object ThisToken : KeywordToken("this")
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

sealed class ContainingOperatorToken(symbol: String, precedence: Int) : InfixOperatorToken(symbol, precedence)
object InToken : ContainingOperatorToken("in", 9), TypeVariableModifier {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        VirtualCall(second as IExpression, "contains", listOf(first as IExpression))
}

object NotInToken : ContainingOperatorToken("!in", 9) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        VirtualCall(VirtualCall(second as IExpression, "contains", listOf(first as IExpression)), "not")
}

sealed class TypeOperatorToken(symbol: String, precedence: Int) : InfixOperatorToken(symbol, precedence)
object IsToken : TypeOperatorToken("is", 9) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        TypeCheck(first as IExpression, (second as PalmType).path)
}

object IsNotToken : TypeOperatorToken("!is", 9) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        VirtualCall(TypeCheck(first as IExpression, (second as PalmType).path), "not")
}

object AsToken : TypeOperatorToken("as", 15) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        Cast(first as IExpression, (second as PalmType).path)
}

private val specialWords = mapOf(
    "true" to TrueToken,
    "false" to FalseToken,
    "null" to NullToken,
    "if" to IfToken,
    "else" to ElseToken,
    "when" to WhenToken,
    "for" to ForToken,
    "in" to InToken,
    "is" to IsToken,
    "as" to AsToken,
    "yield" to YieldToken,
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
    "impl" to ImplToken,
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
    "iterator" to IteratorToken,
    "tailrec" to TailrecToken,
    "const" to ConstToken,
    "companion" to CompanionToken,
    "inner" to InnerToken,
    "sealed" to SealedToken,
    "autofun" to AutofunToken,
    "noinline" to NoinlineToken
)

fun handleUncapitalizedString(string: String) = specialWords[string] ?: IdentifierToken(string)