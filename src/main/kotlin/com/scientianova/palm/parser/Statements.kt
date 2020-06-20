package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.*

sealed class Statement
typealias PStatement = Positioned<Statement>

data class ExpressionStatement(val expr: Expression) : Statement()()

data class ConstDef(
    val name: PString,
    val type: PType,
    val constraints: List<PType>
) : Statement()

data class ConstAssignment(
    val pattern: PDecPattern,
    val expr: PExpr,
    val declaration: Boolean
) : Statement()

data class FunctionAssignment(
    val name: PString,
    val params: List<PExpr>,
    val expr: PExpr,
    val declaration: Boolean
) : Statement()

data class RecordProperty(
    val name: PString,
    val type: PType,
    val mutable: Boolean
)

data class RecordDeclaration(
    val name: PString,
    val genericPool: List<PString>,
    val properties: List<RecordProperty>
) : Statement()

data class EnumCase(
    val name: PString,
    val args: List<PType>
)

data class EnumDec(
    val name: PString,
    val genericPool: List<PString>,
    val cases: List<EnumCase>
) : Statement()

data class AliasDec(
    val name: PString,
    val genericPool: List<PString>,
    val actualType: PType
) : Statement()

enum class Associativity { LEFT, RIGHT, NONE }

data class InfixOperatorDec(
    val symbol: PString,
    val name: PString,
    val associativity: Associativity,
    val precedence: Int
) : Statement()

data class PostfixOperatorDec(
    val symbol: PString,
    val name: PString
) : Statement()

data class PrefixOperatorDec(
    val symbol: PString,
    val name: PString
) : Statement()

data class ClassDec(
    val name: PString,
    val genericPool: List<PString>,
    val constraints: List<PType>,
    val declarations: List<PStatement>
) : Statement()

data class InstanceDec(
    val name: PString,
    val type: PType,
    val constraints: List<PType>,
    val declarations: List<PStatement>
) : Statement()

fun handleDeclaration(
    state: ParseState,
    start: StringPos
): Pair<PStatement, ParseState> = when (val value = token?.value) {
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
                ) at start..identifier.area.last to parser.pop()
            }
            "infix" -> {
                val maybeAssoc = parser.pop()
                val (associativity, maybePrec) = when (maybeAssoc?.value) {
                    LEFT_TOKEN -> Associativity.LEFT to parser.pop()
                    RIGHT_TOKEN -> Associativity.RIGHT to parser.pop()
                    NONE_TOKEN -> Associativity.NONE to parser.pop()
                    else -> Associativity.LEFT to maybeAssoc
                }
                val (precedence, equals) = when (val precValue = maybePrec?.value) {
                    is IntToken -> precValue.value to parser.pop()
                    else -> 7 to maybePrec
                }

                if (precedence !in 0..15) parser.error(INVALID_PRECEDENCE, maybePrec?.area ?: parser.lastArea)
                if (equals?.value !is EqualsToken) parser.error(MISSING_EQUALS_ERROR, equals?.area ?: parser.lastArea)

                val identifierToken = parser.pop()
                val identifier = identifierToken
                    ?: parser.error(INVALID_PROPERTY_NAME_ERROR, identifierToken?.area ?: parser.lastArea)
                if (identifier.value !is IdentifierToken)
                    parser.error(INVALID_PROPERTY_NAME_ERROR, identifierToken.area)

                InfixOperatorDec(
                    afterIdent.map { value.name }, identifier.map { value.name }, associativity, precedence
                ) at start..identifier.area.last to parser.pop()
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
                ) at start..identifier.area.last to parser.pop()
            }
            else -> parser.error(INVALID_FIXITY_ERROR, token.area)
        } else if (value.capitalized) {
            val (genericPool, symbol) = handleFullGenericPool(afterIdent, parser, emptyList())
            when (symbol?.value) {
                RECORD_TOKEN -> {
                    val paren = parser.pop()
                    if (paren?.value is OpenParenToken) handleRecord(
                        parser.pop(), parser, start, value.name at token.area, genericPool, emptyList()
                    ) else parser.error(MISSING_PAREN_AFTER_RECORD_ERROR, paren?.area?.start ?: parser.lastPos)
                }
                ENUM_TOKEN -> {
                    val bracket = parser.pop()
                    if (bracket?.value is OpenCurlyBracketToken)
                        handleEnum(parser.pop(), parser, start, value.name at token.area, genericPool, emptyList())
                    else parser.error(MISSING_BRACKET_AFTER_ENUM_ERROR, bracket?.area?.start ?: parser.lastPos)
                }
                CLASS_TOKEN -> {
                    val maybeWhere = parser.pop()
                    val (constraints, bracket) = if (maybeWhere?.value == WHERE_TOKEN) {
                        val (type, next) = handleType(parser.pop(), parser)
                        (if (type.value is TupleType) type.value.types else listOf(type)) to next
                    } else emptyList<PType>() to maybeWhere
                    if (bracket?.value is OpenCurlyBracketToken) handleClass(
                        parser.pop(), parser, start, value.name at token.area, genericPool, constraints, emptyList()
                    ) else parser.error(MISSING_BRACKET_AFTER_CLASS_ERROR, bracket?.area?.start ?: parser.lastPos)
                }
                IMPL_TOKEN -> {
                    val (forType, maybeWhere) = handleType(parser.pop(), parser)
                    val (constraints, bracket) = if (maybeWhere?.value == WHERE_TOKEN) {
                        val (type, next) = handleType(parser.pop(), parser)
                        (if (type.value is TupleType) type.value.types else listOf(type)) to next
                    } else emptyList<PType>() to maybeWhere
                    if (bracket?.value is OpenCurlyBracketToken) handleInstance(
                        parser.pop(), parser, start, value.name at token.area, forType, constraints, emptyList()
                    ) else parser.error(MISSING_BRACKET_AFTER_IMPL_ERROR, bracket?.area?.start ?: parser.lastPos)
                }
                is EqualsToken -> {
                    val (type, next) = handleType(parser.pop(), parser)
                    AliasDec(value.name at token.area, genericPool, type) at start..type.area.last to next
                }
                else -> parser.error(UNKNOWN_DECLARATION_ERROR, symbol?.area ?: parser.lastArea)
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
                    ConstDef(value.name at token.area, type, constants) at start..type.area.last to next
                }
                is EqualsToken -> {
                    val (expr, next) = handleInScopeExpression(parser.pop(), parser)
                    ConstAssignment(DecNamePattern(value.name) at token.area, expr, true) at
                            start..expr.area.last to next
                }
                is OpenParenToken -> {
                    val (params, equals) = handleParams(parser.pop(), parser, start)
                    if (equals?.value !is EqualsToken)
                        parser.error(MISSING_EQUALS_ERROR, equals?.area?.start ?: parser.lastPos)
                    val (expr, next) = handleInScopeExpression(parser.pop(), parser)
                    FunctionAssignment(value.name at token.area, params.value, expr, true) at
                            start..expr.area.last to next
                }
                else -> parser.error(UNKNOWN_DECLARATION_ERROR, afterIdent?.area ?: parser.lastArea)
            }
        }
    }
    is WildcardToken -> {
        val equals = parser.pop()
        if (equals?.value !is EqualsToken)
            parser.error(MISSING_EQUALS_ERROR, equals?.area?.start ?: parser.lastPos)
        handleInScopeExpression(parser.pop(), parser)
    }
    is OpenParenToken -> {
        val (pattern, equals) =
            handleTupleDecPattern(
                parser.pop(), parser, INVALID_DESTRUCTURED_DECLARATION_ERROR, token.area.first, emptyList()
            )
        if (equals?.value !is EqualsToken)
            parser.error(MISSING_EQUALS_ERROR, equals?.area?.start ?: parser.lastPos)
        val (expr, next) = handleInScopeExpression(parser.pop(), parser)
        ConstAssignment(pattern, expr, true) at start..expr.area.last to next
    }
    else -> parser.error(UNKNOWN_DECLARATION_ERROR, token?.area ?: parser.lastArea)
}

tailrec fun handleRecord(
    state: ParseState,
    start: StringPos,
    name: PString,
    genericPool: List<PString>,
    list: List<RecordProperty>
): Pair<Positioned<RecordDeclaration>, ParseState> = if (token?.value is ClosedParenToken) {
    if (list.isEmpty()) parser.error(EMPTY_RECORD_ERROR, token.area)
    RecordDeclaration(name, genericPool, list) at start..token.area.last to parser.pop()
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
            RecordProperty(identifier.value.name at identifier.area, type, true) to next
        }
        colon?.value is ColonToken -> {
            val (type, next) = handleType(parser.pop(), parser)
            RecordProperty(identifier.value.name at identifier.area, type, false) to next
        }
        else -> parser.error(MISSING_COLON_IN_PROPERTY_ERROR, colon?.area ?: parser.lastArea)
    }
    when (next?.value) {
        is CommaToken ->
            handleRecord(parser.pop(), parser, start, name, genericPool, list + property)
        is ClosedParenToken ->
            RecordDeclaration(name, genericPool, list + property) at start..next.area.last to parser.pop()
        else ->
            parser.error(UNCLOSED_PARENTHESIS_ERROR, next?.area?.start ?: parser.lastPos)
    }
}

tailrec fun handleEnum(
    state: ParseState
    start: StringPos,
    name: PString,
    genericPool: List<PString>,
    cases: List<EnumCase>
): Pair<Positioned<EnumDec>, ParseState> = if (token?.value is ClosedCurlyBracketToken) {
    EnumDec(name, genericPool, cases) at start..token.area.last to parser.pop()
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
                    EnumDec(name, genericPool, cases + case) at start..symbol.area.last to parser.pop()
                else -> parser.error(UNCLOSED_ENUM_ERROR, next?.area?.start ?: parser.lastPos)
            }
        }
        is CommaToken -> handleEnum(
            parser.pop(), parser, start, name, genericPool,
            cases + EnumCase(identifier.value.name at identifier.area, emptyList())
        )
        is ClosedCurlyBracketToken -> EnumDec(
            name, genericPool, cases + EnumCase(identifier.value.name at identifier.area, emptyList())
        ) at start..symbol.area.last to parser.pop()
        else -> parser.error(UNCLOSED_ENUM_ERROR, symbol?.area?.start ?: parser.lastPos)
    }
}

tailrec fun handleCaseArguments(
    state: ParseState,
    name: PString,
    types: List<PType>
): Pair<EnumCase, ParseState> = if (token?.value is ClosedParenToken) {
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
    state: ParseState,
    start: StringPos,
    name: PString,
    genericPool: List<PString>,
    constants: List<PType>,
    declarations: List<PStatement>
): Pair<PStatement, ParseState> = if (token?.value is ClosedCurlyBracketToken) {
    ClassDec(name, genericPool, constants, declarations) at start..token.area.last to parser.pop()
} else {
    val (statement, next) = when (val value = token?.value) {
        is LetToken -> handleDeclaration(parser.pop(), parser, token.area.first)
        is IdentifierToken -> {
            val symbol = parser.pop()
            when (symbol?.value) {
                is EqualsToken -> {
                    val (expr, next) = handleInScopeExpression(parser.pop(), parser)
                    ConstAssignment(
                        DecNamePattern(value.name) at token.area, expr, false
                    ) at token.area.first..expr.area.last to next
                }
                is OpenParenToken -> {
                    val (params, equals) = handleParams(parser.pop(), parser, symbol.area.first, emptyList())
                    if (equals?.value !is EqualsToken)
                        parser.error(MISSING_EQUALS_ERROR, equals?.area ?: parser.lastArea)
                    val (expr, next) = handleInScopeExpression(parser.pop(), parser)
                    FunctionAssignment(value.name at token.area, params.value, expr, false) at
                            token.area.first..expr.area.last to next
                }
                else -> parser.error(UNKNOWN_DECLARATION_ERROR, symbol?.area ?: parser.lastArea)
            }
        }
        is OpenParenToken -> {
            val (pattern, equals) = handleTupleDecPattern(
                parser.pop(), parser, INVALID_DESTRUCTURED_DECLARATION_ERROR, token.area.first, emptyList()
            )
            if (equals?.value !is EqualsToken) parser.error(MISSING_EQUALS_ERROR, equals?.area ?: parser.lastArea)
            val (expr, next) = handleInScopeExpression(parser.pop(), parser)
            ConstAssignment(pattern, expr, false) at token.area.first..expr.area.last to next
        }
        else -> parser.error(UNKNOWN_DECLARATION_ERROR, token?.area ?: parser.lastArea)
    }
    handleClass(
        if (next?.value is SeparatorToken) parser.pop() else next, parser, start,
        name, genericPool, constants, declarations + statement
    )
}

tailrec fun handleInstance(
    state: ParseState,
    start: StringPos,
    name: PString,
    type: PType,
    constants: List<PType>,
    declarations: List<PStatement>
): Pair<PStatement, ParseState> = if (token?.value is ClosedCurlyBracketToken) {
    InstanceDec(name, type, constants, declarations) at start..token.area.last to parser.pop()
} else {
    val (statement, next) = when (val value = token?.value) {
        is IdentifierToken -> {
            val symbol = parser.pop()
            when (symbol?.value) {
                is EqualsToken -> {
                    val (expr, next) = handleInScopeExpression(parser.pop(), parser)
                    ConstAssignment(
                        DecNamePattern(value.name) at token.area, expr, false
                    ) at token.area.first..expr.area.last to next
                }
                is OpenParenToken -> {
                    val (params, equals) = handleParams(parser.pop(), parser, symbol.area.first, emptyList())
                    if (equals?.value !is EqualsToken)
                        parser.error(MISSING_EQUALS_ERROR, equals?.area ?: parser.lastArea)
                    val (expr, next) = handleInScopeExpression(parser.pop(), parser)
                    FunctionAssignment(value.name at token.area, params.value, expr, false) at
                            token.area.first..expr.area.last to next
                }
                else -> parser.error(UNKNOWN_DECLARATION_ERROR, symbol?.area ?: parser.lastArea)
            }
        }
        is OpenParenToken -> {
            val (pattern, equals) = handleTupleDecPattern(
                parser.pop(), parser, INVALID_DESTRUCTURED_DECLARATION_ERROR, token.area.first, emptyList()
            )
            if (equals?.value !is EqualsToken) parser.error(MISSING_EQUALS_ERROR, equals?.area ?: parser.lastArea)
            val (expr, next) = handleInScopeExpression(parser.pop(), parser)
            ConstAssignment(pattern, expr, false) at token.area.first..expr.area.last to next
        }
        else -> parser.error(UNKNOWN_DECLARATION_ERROR, token?.area ?: parser.lastArea)
    }
    handleInstance(
        if (next?.value is SeparatorToken) parser.pop() else next, parser, start,
        name, type, constants, declarations + statement
    )
}

tailrec fun handleFullGenericPool(
    state: ParseState,
    pool: List<PString>
): Pair<List<PString>, ParseState> = if (token?.value is OpenSquareBracketToken) {
    val (newPool, next) = handleGenericPool(parser.pop(), parser, pool)
    handleFullGenericPool(next, parser, newPool)
} else pool to token

tailrec fun handleGenericPool(
    state: ParseState,
    pool: List<PString>
): Pair<List<PString>, ParseState> = if (token?.value is ClosedSquareBracketToken) pool to parser.pop() else {
    val identifier = token ?: parser.error(INVALID_GENERIC_VARIABLE_ERROR, token?.area ?: parser.lastArea)
    if (identifier.value !is IdentifierToken) parser.error(INVALID_ENUM_CASE_NAME_ERROR, token.area)
    val variable = identifier.value.name at identifier.area
    val symbol = parser.pop()
    when (symbol?.value) {
        is CommaToken -> handleGenericPool(parser.pop(), parser, pool + variable)
        is ClosedSquareBracketToken -> pool + variable to parser.pop()
        else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, symbol?.area ?: parser.lastArea)
    }
}