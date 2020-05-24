package com.scientianova.palm.tokenizer

object ElseToken : IdentifierToken("else")
object WhereToken : IdentifierToken("where")
object MutToken : IdentifierToken("mut")
object AsToken : IdentifierToken("as")

sealed class DeclarationToken(name: String) : IdentifierToken(name)
object AliasToken : DeclarationToken("alias")
object ClassToken : DeclarationToken("enum")
object EnumToken : DeclarationToken("enum")
object RecordToken : DeclarationToken("record")
object ObjectToken : DeclarationToken("object")
object OperatorToken : DeclarationToken("operator")
object ImplToken : DeclarationToken("impl")