package com.scientianova.palm.parser

data class ParsedModule(
    val statements: List<PStatement> = emptyList(),
    val constants: Map<String, PStatement> = emptyMap(),
    val types: Map<String, PStatement> = emptyMap(),
    val classes: Map<String, PStatement> = emptyMap(),
    val instances: Map<String, PStatement> = emptyMap(),
    val prefixOps: Map<String, PrefixOperatorDec> = emptyMap(),
    val infixOps: Map<String, InfixOperatorDec> = emptyMap(),
    val postfixOps: Map<String, PostfixOperatorDec> = emptyMap()
) {
    operator fun plus(other: ParsedModule) = ParsedModule(
        statements + other.statements,
        constants + other.constants,
        types + other.types,
        classes + other.classes,
        instances + other.instances,
        prefixOps + other.prefixOps,
        infixOps + other.infixOps,
        postfixOps + other.postfixOps
    )

    fun with(statement: PStatement): ParsedModule = when (statement.value) {
        is ConstDef -> copy(
            statements = statements + statement,
            constants = constants + (statement.value.name.value to statement)
        )
        is ConstAssignment -> copy(
            statements = statements + statement,
            constants = if (statement.value.declaration)
                constants + getNamesInPattern(statement.value.pattern.value).map { it to statement }
            else constants
        )
        is FunctionAssignment -> copy(
            statements = statements + statement,
            constants = if (statement.value.declaration) constants + (statement.value.name.value to statement) else constants
        )
        is RecordDeclaration -> copy(
            statements = statements + statement,
            types = types + (statement.value.name.value to statement),
            constants = constants + (statement.value.name.value to statement) + statement.value.properties.map {
                it.name.value to statement
            }
        )
        is EnumDec -> copy(
            statements = statements + statement,
            types = types + (statement.value.name.value to statement),
            constants = constants + statement.value.cases.map {
                it.name.value to statement
            }
        )
        is ClassDec -> statement.value.declarations.fold(
            copy(
                statements = statements + statement,
                classes = classes + (statement.value.name.value to statement)
            )
        ) { acc, curr -> acc.with(curr) }
        is AliasDec -> copy(
            statements = statements + statement,
            types = constants + (statement.value.name.value to statement)
        )
        is InstanceDec -> copy(
            statements = statements + statement,
            instances = constants + (statement.value.name.value to statement)
        )
        is PrefixOperatorDec -> copy(
            prefixOps = prefixOps + (statement.value.symbol.value to statement.value)
        )
        is InfixOperatorDec -> copy(
            infixOps = infixOps + (statement.value.symbol.value to statement.value)
        )
        is PostfixOperatorDec -> copy(
            postfixOps = postfixOps + (statement.value.symbol.value to statement.value)
        )
        else -> copy(statements = statements + statement)
    }
}