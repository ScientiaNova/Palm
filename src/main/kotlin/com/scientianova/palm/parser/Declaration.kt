package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.tokenizer.*
import com.scientianova.palm.util.*

sealed class Declaration : IStatement
typealias PDeclaration = Positioned<Declaration>

data class ConstDef(
    val name: PString,
    val type: PType,
    val constraints: List<PType>
) : Declaration()

data class ConstAssignment(
    val pattern: PDecPattern,
    val expr: PExpression,
    val declaration: Boolean
) : Declaration()

data class FunctionAssignment(
    val name: PString,
    val params: List<PExpression>,
    val expr: PExpression,
    val declaration: Boolean
) : Declaration()

data class RecordProperty(
    val name: PString,
    val type: PType,
    val mutable: Boolean
)

data class RecordDeclaration(
    val name: PString,
    val genericPool: List<PString>,
    val properties: List<RecordProperty>
) : Declaration()

data class EnumCase(
    val name: PString,
    val args: List<PType>
)

data class EnumDec(
    val name: PString,
    val genericPool: List<PString>,
    val cases: List<EnumCase>
) : Declaration()

data class AliasDec(
    val name: PString,
    val genericPool: List<PString>,
    val actualType: PType
) : Declaration()

enum class Associativity { LEFT, RIGHT, NONE }

data class InfixOperatorDec(
    val symbol: PString,
    val name: PString,
    val associativity: Associativity,
    val precedence: Int
) : Declaration()

data class PostfixOperatorDec(
    val symbol: PString,
    val name: PString
) : Declaration()

data class PrefixOperatorDec(
    val symbol: PString,
    val name: PString
) : Declaration()

data class ClassDec(
    val name: PString,
    val genericPool: List<PString>,
    val constraints: List<PType>,
    val declarations: List<PStatement>
) : Declaration()

data class InstanceDec(
    val name: PString,
    val type: PType,
    val constraints: List<PType>,
    val declarations: List<PStatement>
) : Declaration()

fun handleDeclaration(
    token: PToken?,
    parser: Parser,
    start: StringPos
): Pair<PStatement, PToken?> = when (val value = token?.value) {
    is IdentifierToken -> {
        val afterIdent = parser.pop()
        if (afterIdent != null && afterIdent.value is InfixOperatorToken) when (value.name) {
            "prefix" -> {
                val equals = parser.pop()
                if (equals?.value !is EqualsToken) parser.error(MISSING_EQUALS_ERROR, equals?.area ?: parser.lastArea)
                val identifierToken = parser.pop()
                val identifier = identifierToken
                    ?: parser.error(INVALID_PROPERTY_NAME_ERROR, identifierToken?.area ?: parser.lastArea)
                if (identifier.value !is IdentifierToken)
                    parser.error(INVALID_PROPERTY_NAME_ERROR, identifierToken.area)
                PrefixOperatorDec(
                    afterIdent.map { value.name }, identifier.map { value.name }
                ) on start..identifier.area.end to parser.pop()
            }
            "infix" -> {
                val maybeAssoc = parser.pop()
                val (associativity, maybePrec) = when (maybeAssoc?.value) {
                    LEFT_TOKEN -> Associativity.LEFT to parser.pop()
                    RIGHT_TOKEN -> Associativity.RIGHT to parser.pop()
                    NONE_TOKEN -> Associativity.NONE to parser.pop()
                    is EqualsToken -> Associativity.LEFT to maybeAssoc
                    else -> parser.error(MISSING_EQUALS_ERROR, maybeAssoc?.area ?: parser.lastArea)
                }
                val (precedence, equals) = when (val precValue = maybePrec?.value) {
                    is IntToken -> precValue.value to parser.pop()
                    is EqualsToken -> 7 to maybePrec
                    else -> parser.error(MISSING_EQUALS_ERROR, maybePrec?.area ?: parser.lastArea)
                }

                if (precedence !in 0..15) parser.error(INVALID_PRECEDENCE, maybePrec.area)
                if (equals?.value !is EqualsToken) parser.error(MISSING_EQUALS_ERROR, equals?.area ?: parser.lastArea)

                val identifierToken = parser.pop()
                val identifier = identifierToken
                    ?: parser.error(INVALID_PROPERTY_NAME_ERROR, identifierToken?.area ?: parser.lastArea)
                if (identifier.value !is IdentifierToken)
                    parser.error(INVALID_PROPERTY_NAME_ERROR, identifierToken.area)

                InfixOperatorDec(
                    afterIdent.map { value.name }, identifier.map { value.name }, associativity, precedence
                ) on start..identifier.area.end to parser.pop()
            }
            "postfix" -> {
                val equals = parser.pop()
                if (equals?.value !is EqualsToken) parser.error(MISSING_EQUALS_ERROR, equals?.area ?: parser.lastArea)
                val identifierToken = parser.pop()
                val identifier = identifierToken
                    ?: parser.error(INVALID_PROPERTY_NAME_ERROR, identifierToken?.area ?: parser.lastArea)
                if (identifier.value !is IdentifierToken)
                    parser.error(INVALID_PROPERTY_NAME_ERROR, identifierToken.area)
                PostfixOperatorDec(
                    afterIdent.map { value.name }, identifier.map { value.name }
                ) on start..identifier.area.end to parser.pop()
            }
            else -> parser.error(INVALID_FIXITY_ERROR, token.area)
        } else if (value.capitalized) {
            val (genericPool, symbol) = handleFullGenericPool(afterIdent, parser, emptyList())
            when (symbol?.value) {
                RECORD_TOKEN -> {
                    val paren = parser.pop()
                    if (paren?.value is OpenParenToken) handleRecord(
                        parser.pop(), parser, start, value.name on token.area, genericPool, emptyList()
                    ) else parser.error(MISSING_PAREN_AFTER_RECORD_ERROR, paren?.area?.start ?: parser.lastPos)
                }
                ENUM_TOKEN -> {
                    val bracket = parser.pop()
                    if (bracket?.value is OpenCurlyBracketToken)
                        handleEnum(parser.pop(), parser, start, value.name on token.area, genericPool, emptyList())
                    else parser.error(MISSING_BRACKET_AFTER_ENUM_ERROR, bracket?.area?.start ?: parser.lastPos)
                }
                CLASS_TOKEN -> {
                    val maybeWhere = parser.pop()
                    val (constraints, bracket) = if (maybeWhere?.value == WHERE_TOKEN) {
                        val (type, next) = handleType(parser.pop(), parser)
                        (if (type.value is TupleType) type.value.types else listOf(type)) to next
                    } else emptyList<PType>() to maybeWhere
                    if (bracket?.value is OpenCurlyBracketToken) handleClass(
                        parser.pop(), parser, start, value.name on token.area, genericPool, constraints, emptyList()
                    ) else parser.error(MISSING_BRACKET_AFTER_CLASS_ERROR, bracket?.area?.start ?: parser.lastPos)
                }
                IMPL_TOKEN -> {
                    val (forType, maybeWhere) = handleType(parser.pop(), parser)
                    val (constraints, bracket) = if (maybeWhere?.value == WHERE_TOKEN) {
                        val (type, next) = handleType(parser.pop(), parser)
                        (if (type.value is TupleType) type.value.types else listOf(type)) to next
                    } else emptyList<PType>() to maybeWhere
                    if (bracket?.value is OpenCurlyBracketToken) handleInstance(
                        parser.pop(), parser, start, value.name on token.area, forType, constraints, emptyList()
                    ) else parser.error(MISSING_BRACKET_AFTER_IMPL_ERROR, bracket?.area?.start ?: parser.lastPos)
                }
                is EqualsToken -> {
                    val (type, next) = handleType(parser.pop(), parser)
                    AliasDec(value.name on token.area, genericPool, type) on start..type.area.end to next
                }
                else -> parser.error(UNKNOWNN_DECLARATION_ERROR, symbol?.area ?: parser.lastArea)
            }
        } else {
            when (afterIdent?.value) {
                is ColonToken -> {
                    val (type, maybeWhere) = handleType(parser.pop(), parser)
                    val (constants, next) = if (maybeWhere?.value == WHERE_TOKEN) {
                        val (constantType, next) = handleType(parser.pop(), parser)
                        if (constantType.value is TupleType) constantType.value.types to next
                        else listOf(constantType) to next
                    } else emptyList<PType>() to maybeWhere
                    ConstDef(value.name on token.area, type, constants) on start..type.area.end to next
                }
                is EqualsToken -> {
                    val (expr, next) = handleExpression(parser.pop(), parser)
                    ConstAssignment(DecNamePattern(value.name) on token.area, expr, true) on
                            start..expr.area.end to next
                }
                is OpenParenToken -> {
                    val (params, equals) = handleParams(parser.pop(), parser, start)
                    if (equals?.value !is EqualsToken)
                        parser.error(MISSING_EQUALS_ERROR, equals?.area?.start ?: parser.lastPos)
                    val (expr, next) = handleExpression(parser.pop(), parser)
                    FunctionAssignment(value.name on token.area, params.value, expr, true) on
                            start..expr.area.end to next
                }
                else -> parser.error(UNKNOWNN_DECLARATION_ERROR, afterIdent?.area ?: parser.lastArea)
            }
        }
    }
    is WildcardToken -> {
        val equals = parser.pop()
        if (equals?.value !is EqualsToken)
            parser.error(MISSING_EQUALS_ERROR, equals?.area?.start ?: parser.lastPos)
        handleExpression(parser.pop(), parser)
    }
    is OpenParenToken -> {
        val (pattern, equals) =
            handleTupleDecPattern(
                parser.pop(),
                parser,
                INVALID_DESTRUCTURED_DECLARATION_ERROR,
                token.area.start,
                emptyList()
            )
        if (equals?.value !is EqualsToken)
            parser.error(MISSING_EQUALS_ERROR, equals?.area?.start ?: parser.lastPos)
        val (expr, next) = handleExpression(parser.pop(), parser)
        ConstAssignment(pattern, expr, true) on start..expr.area.end to next
    }
    else -> parser.error(UNKNOWNN_DECLARATION_ERROR, token?.area ?: parser.lastArea)
}

tailrec fun handleRecord(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    name: PString,
    genericPool: List<PString>,
    list: List<RecordProperty>
): Pair<Positioned<RecordDeclaration>, PToken?> = if (token?.value is ClosedParenToken) {
    if (list.isEmpty()) parser.error(EMPTY_RECORD_ERROR, token.area)
    RecordDeclaration(name, genericPool, list) on start..token.area.end to parser.pop()
} else {
    val identifier = token ?: parser.error(INVALID_PROPERTY_NAME_ERROR, token?.area ?: parser.lastArea)
    if (identifier.value !is IdentifierToken) parser.error(INVALID_PROPERTY_NAME_ERROR, token.area)
    val colon = parser.pop()
    val (property, next) = when {
        identifier.value.name == "mut" && colon != null && colon.value is IdentifierToken -> {
            val actualColon = parser.pop()
            if (actualColon?.value !is ColonToken)
                parser.error(MISSING_COLON_IN_PROPERTY_ERROR, actualColon?.area ?: parser.lastArea)
            val (type, next) = handleType(parser.pop(), parser)
            RecordProperty(identifier.value.name on identifier.area, type, true) to next
        }
        colon?.value is ColonToken -> {
            val (type, next) = handleType(parser.pop(), parser)
            RecordProperty(identifier.value.name on identifier.area, type, false) to next
        }
        else -> parser.error(MISSING_COLON_IN_PROPERTY_ERROR, colon?.area ?: parser.lastArea)
    }
    when (next?.value) {
        is CommaToken ->
            handleRecord(parser.pop(), parser, start, name, genericPool, list + property)
        is ClosedParenToken ->
            RecordDeclaration(name, genericPool, list + property) on start..next.area.end to parser.pop()
        else ->
            parser.error(UNCLOSED_PARENTHESIS_ERROR, next?.area?.start ?: parser.lastPos)
    }
}

tailrec fun handleEnum(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    name: PString,
    genericPool: List<PString>,
    cases: List<EnumCase>
): Pair<Positioned<EnumDec>, PToken?> = if (token?.value is ClosedCurlyBracketToken) {
    EnumDec(name, genericPool, cases) on start..token.area.end to parser.pop()
} else {
    val identifier = token ?: parser.error(INVALID_ENUM_CASE_NAME_ERROR, token?.area ?: parser.lastArea)
    if (identifier.value !is IdentifierToken) parser.error(INVALID_ENUM_CASE_NAME_ERROR, token.area)
    val symbol = parser.pop()
    when (symbol?.value) {
        is OpenParenToken -> {
            val (case, next) = handleCaseArguments(parser.pop(), parser, name, emptyList())
            when (next?.value) {
                is CommaToken ->
                    handleEnum(parser.pop(), parser, start, name, genericPool, cases + case)
                is ClosedCurlyBracketToken ->
                    EnumDec(name, genericPool, cases + case) on start..symbol.area.end to parser.pop()
                else -> parser.error(UNCLOSED_ENUM_ERROR, next?.area?.start ?: parser.lastPos)
            }
        }
        is CommaToken -> handleEnum(
            parser.pop(), parser, start, name, genericPool,
            cases + EnumCase(identifier.value.name on identifier.area, emptyList())
        )
        is ClosedCurlyBracketToken -> EnumDec(
            name, genericPool, cases + EnumCase(identifier.value.name on identifier.area, emptyList())
        ) on start..symbol.area.end to parser.pop()
        else -> parser.error(UNCLOSED_ENUM_ERROR, symbol?.area?.start ?: parser.lastPos)
    }
}

tailrec fun handleCaseArguments(
    token: PToken?,
    parser: Parser,
    name: PString,
    types: List<PType>
): Pair<EnumCase, PToken?> = if (token?.value is ClosedParenToken) {
    EnumCase(name, types) to parser.pop()
} else {
    val (type, symbol) = handleType(token, parser)
    when (symbol?.value) {
        is CommaToken -> handleCaseArguments(parser.pop(), parser, name, types + type)
        is ClosedParenToken -> EnumCase(name, types + type) to parser.pop()
        else -> parser.error(UNCLOSED_PARENTHESIS_ERROR, symbol?.area ?: parser.lastArea)
    }
}

tailrec fun handleClass(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    name: PString,
    genericPool: List<PString>,
    constants: List<PType>,
    declarations: List<PStatement>
): Pair<PStatement, PToken?> = if (token?.value is ClosedCurlyBracketToken) {
    ClassDec(name, genericPool, constants, declarations) on start..token.area.end to parser.pop()
} else {
    val (statement, next) = when (val value = token?.value) {
        is LetToken -> handleDeclaration(parser.pop(), parser, token.area.start)
        is IdentifierToken -> {
            val symbol = parser.pop()
            when (symbol?.value) {
                is EqualsToken -> {
                    val (expr, next) = handleExpression(token, parser)
                    ConstAssignment(
                        DecNamePattern(value.name) on token.area, expr, false
                    ) on token.area.start..expr.area.end to next
                }
                is OpenParenToken -> {
                    val (params, equals) = handleParams(parser.pop(), parser, symbol.area.start, emptyList())
                    if (equals?.value !is EqualsToken)
                        parser.error(MISSING_EQUALS_ERROR, equals?.area ?: parser.lastArea)
                    val (expr, next) = handleExpression(parser.pop(), parser)
                    FunctionAssignment(value.name on token.area, params.value, expr, false) on
                            token.area.start..expr.area.end to next
                }
                else -> parser.error(UNKNOWNN_DECLARATION_ERROR, symbol?.area ?: parser.lastArea)
            }
        }
        is OpenParenToken -> {
            val (pattern, equals) = handleTupleDecPattern(
                parser.pop(), parser, INVALID_DESTRUCTURED_DECLARATION_ERROR, token.area.start, emptyList()
            )
            if (equals?.value !is EqualsToken) parser.error(MISSING_EQUALS_ERROR, equals?.area ?: parser.lastArea)
            val (expr, next) = handleExpression(parser.pop(), parser)
            ConstAssignment(pattern, expr, false) on token.area.start..expr.area.end to next
        }
        else -> parser.error(UNKNOWNN_DECLARATION_ERROR, token?.area ?: parser.lastArea)
    }
    handleClass(
        if (next?.value is SeparatorToken) parser.pop() else next, parser, start,
        name, genericPool, constants, declarations + statement
    )
}

tailrec fun handleInstance(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    name: PString,
    type: PType,
    constants: List<PType>,
    declarations: List<PStatement>
): Pair<PStatement, PToken?> = if (token?.value is ClosedCurlyBracketToken) {
    InstanceDec(name, type, constants, declarations) on start..token.area.end to parser.pop()
} else {
    val (statement, next) = when (val value = token?.value) {
        is IdentifierToken -> {
            val symbol = parser.pop()
            when (symbol?.value) {
                is EqualsToken -> {
                    val (expr, next) = handleExpression(token, parser)
                    ConstAssignment(
                        DecNamePattern(value.name) on token.area, expr, false
                    ) on token.area.start..expr.area.end to next
                }
                is OpenParenToken -> {
                    val (params, equals) = handleParams(parser.pop(), parser, symbol.area.start, emptyList())
                    if (equals?.value !is EqualsToken)
                        parser.error(MISSING_EQUALS_ERROR, equals?.area ?: parser.lastArea)
                    val (expr, next) = handleExpression(parser.pop(), parser)
                    FunctionAssignment(value.name on token.area, params.value, expr, false) on
                            token.area.start..expr.area.end to next
                }
                else -> parser.error(UNKNOWNN_DECLARATION_ERROR, symbol?.area ?: parser.lastArea)
            }
        }
        is OpenParenToken -> {
            val (pattern, equals) = handleTupleDecPattern(
                parser.pop(), parser, INVALID_DESTRUCTURED_DECLARATION_ERROR, token.area.start, emptyList()
            )
            if (equals?.value !is EqualsToken) parser.error(MISSING_EQUALS_ERROR, equals?.area ?: parser.lastArea)
            val (expr, next) = handleExpression(parser.pop(), parser)
            ConstAssignment(pattern, expr, false) on token.area.start..expr.area.end to next
        }
        else -> parser.error(UNKNOWNN_DECLARATION_ERROR, token?.area ?: parser.lastArea)
    }
    handleInstance(
        if (next?.value is SeparatorToken) parser.pop() else next, parser, start,
        name, type, constants, declarations + statement
    )
}

tailrec fun handleFullGenericPool(
    token: PToken?,
    parser: Parser,
    pool: List<PString>
): Pair<List<PString>, PToken?> = if (token?.value is OpenSquareBracketToken) {
    val (newPool, next) = handleGenericPool(parser.pop(), parser, pool)
    handleFullGenericPool(next, parser, newPool)
} else pool to token

tailrec fun handleGenericPool(
    token: PToken?,
    parser: Parser,
    pool: List<PString>
): Pair<List<PString>, PToken?> = if (token?.value is ClosedSquareBracketToken) pool to parser.pop() else {
    val identifier = token ?: parser.error(INVALID_GENERIC_VARIABLE_ERROR, token?.area ?: parser.lastArea)
    if (identifier.value !is IdentifierToken) parser.error(INVALID_ENUM_CASE_NAME_ERROR, token.area)
    val variable = identifier.value.name on identifier.area
    val symbol = parser.pop()
    when (symbol?.value) {
        is CommaToken -> handleGenericPool(parser.pop(), parser, pool + variable)
        is ClosedSquareBracketToken -> pool + variable to parser.pop()
        else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, symbol?.area ?: parser.lastArea)
    }
}