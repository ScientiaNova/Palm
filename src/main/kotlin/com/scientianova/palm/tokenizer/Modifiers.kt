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
object IteratorToken : SpecialDefinitionToken("iterator")

sealed class TypeModifier(name: String) : IdentifierToken(name), DefinitionModifier
object CompanionToken : TypeModifier("companion")
object InnerToken : TypeModifier("inner")
object SealedToken : TypeModifier("sealed")

interface ArgumentModifier : IToken
object AutofunToken : IdentifierToken("autofn"), ArgumentModifier
object NoinlineToken : IdentifierToken("noinline"), ArgumentModifier