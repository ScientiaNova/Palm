package com.scientianova.palm.tokenizer

interface TypeVariableModifier : IToken
object OutToken : IdentifierToken("out"), TypeVariableModifier

interface DefinitionModifier : IToken

object ConstToken : IdentifierToken("const"), DefinitionModifier
object MutToken : IdentifierToken("mut"), DefinitionModifier
object AbstractToken : IdentifierToken("abstract"), DefinitionModifier
object OverrideToken : IdentifierToken("override"), DefinitionModifier
object LateinitToken : IdentifierToken("lateinit"), DefinitionModifier

sealed class VisibilityModifier(name: String) : IdentifierToken(name), DefinitionModifier
object PrivateToken : VisibilityModifier("private")
object ProtectedToken : VisibilityModifier("protected")
object InternalToken : VisibilityModifier("internal")
object PublicToken : VisibilityModifier("public")

sealed class InheritanceModifier(name: String) : IdentifierToken(name), DefinitionModifier
object OpenToken : InheritanceModifier("open")
object FinalToken : InheritanceModifier("final")

sealed class SpecialDefinitionToken(name: String) : IdentifierToken(name), DefinitionModifier
object InlineToken : SpecialDefinitionToken("inline")
object TailrecToken : SpecialDefinitionToken("tailrec")

sealed class TypeModifier(name: String) : IdentifierToken(name), DefinitionModifier
object CompanionToken : TypeModifier("companion")
object InnerToken : TypeModifier("inner")
object SealedToken : TypeModifier("sealed")

interface ArgumentModifier : IToken
object AutofnToken : IdentifierToken("autofn"), ArgumentModifier
object NoinlineToken : IdentifierToken("noinline"), ArgumentModifier

sealed class OperatorModifier(name: String) : IdentifierToken(name), DefinitionModifier
object PrefixToken : TypeModifier("prefix")
object InfixToken : TypeModifier("infix")
object PostfixToken : TypeModifier("postfix")