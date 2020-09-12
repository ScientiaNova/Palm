package com.scientianova.palm.errors

data class PalmError(val name: String, val context: String, val help: String)

val missingSingleQuote = PalmError(
    "MISSING SINGLE QUOTE",
    "I was expecting a single quote to closed the char literal",
    """
Add a single quote here.

Note: String literals are created with double quotes and char literals are created with single quotes. That way I can better know what you want to use.
    """.trimIndent()
)

val missingSingleQuoteOnQuote = PalmError(
    "MISSING SINGLE QUOTE",
    "I was expecting a single quote to closed the char literal",
    """
Add a single quote here.

Note: Backslashes are used for escape characters. If you want to get the backslash character you need to use \\.
    """.trimIndent()
)

val loneSingleQuote = PalmError(
    "LONE SINGLE QUOTE",
    "I found a single quote by itself at the end of a line",
    """
Was this by mistake? Maybe you want to get the new line character? If it's the latter, use \n.
    """.trimIndent()
)

val malformedTab = PalmError(
    "MALFORMED TAB",
    "I found a lot of spaces in between two single quotes:",
    """
This is most likely caused by your IDE automatically converting tabs into spaces. To say safe, use \t instead.
    """.trimIndent()
)

val unclosedEscapeCharacter = PalmError(
    "INVALID ESCAPE CHARACTER",
    "I saw a backslash, so I was expecting an escape character, but instead got:",
    """
Valid escape characters are:
  \" - double quote
  \$ - money sign
  \\ - backslash
  \t - tab
  \n - new line
  \b - backslash
  \r - carriage return
  \f - form feed
  \v - vertical tab
  \u{code} - character with with a given unicode code
    """.trimIndent()
)

val missingDoubleQuote = PalmError(
    "MISSING DOUBLE QUOTE",
    "I went to the end of this line and couldn't find the end of a single-line string:",
    """
Add a " here.

Note: If you want to use a multi-line string, you need to enclose the string in triple double quotes.
    """.trimIndent()
)

val unclosedMultilineString = PalmError(
    "UNCLOSED MULTI_LINE STRING",
    "I couldn't find the end of this multi-line string:",
    "Closed it off using triple double quotes (\"\"\")."
)

val unclosedParenthesis = PalmError(
    "UNCLOSED PARENTHESIS",
    "I was expecting a closed parenthesis here, but instead got:",
    "Add a ) here."
)

val unclosedSquareBracket = PalmError(
    "UNCLOSED SQUARE BRACKET",
    "I was expecting a closed square bracket here, but instead got:",
    "Add a ] here."
)

val missingBracketInUnicode = PalmError(
    "MISSING CURLY BRACKET",
    "I saw a \\u and was expecting an open curly bracket next, but instead got:",
    "Add an { here."
)

val invalidExponent = PalmError(
    "INVALID EXPONENT",
    "I was going through a decimal literal with scientific notation and was expecting a digit next, but instead got:",
    ""
)

val invalidBinaryLiteral = PalmError(
    "INVALID BINARY LITERAL",
    "I was going through a binary number literal and got:",
    "Binary number literals can only contain the digits 0 and 1."
)

val invalidDecimalLiteral = PalmError(
    "INVALID DECIMAL LITERAL",
    "I was going through a decimal number literal and got:",
    "Decimal number literals can only contain the digits 0-9."
)

val invalidHexLiteral = PalmError(
    "INVALID HEX LITERAL",
    "I was going through a hexadecimal number literal and got:",
    "Hexadecimal number literals can only contain the digits 0-9, a-f and A-F."
)

val missingScope = PalmError(
    "MISSING SCOPE",
    "I was expecting a scope next, but instead got:",
    "Start a scope here with an {."
)

val invalidImport = PalmError(
    "INVALID IMPORT",
    "I was going through an import statement and found:",
    "Imports can only contain paths, that could end with ... or an operator, or could have an alias separated via 'as'."
)

val unclosedImportGroup = PalmError(
    "UNCLOSED IMPORT GROUP",
    "I was going through an import group and instead of a closed curly bracket found:",
    "Add a } here."
)

val unclosedCharLiteral = PalmError(
    "UNCLOSED CHAR LITERAL",
    "I was going through a char literal, but have reached the end of the file without finding its end:",
    "Add a ' here."
)

val unclosedWhen = PalmError(
    "UNCLOSED WHEN EXPRESSION",
    "I was going through a when expression and was expecting a closed curly bracket or expression separator, but instead got:",
    "Add a } here."
)

val unclosedScope = PalmError(
    "UNCLOSED SCOPE",
    "I was going through a scope and was expecting a curly bracket next, but instead got:",
    "Add a } here."
)

val invalidBacktickedIdentifier = PalmError(
    "INVALID BACKTICKED IDENTIFIER",
    "I was going through a backticked identifier and got:",
    "Backticked identifier can use any symbol but /, \\, ., ;, :, <, >, [, ], and line separators."
)

val invalidDoubleDeclaration = PalmError(
    "DOUBLE DECLARATION PATTERN",
    "I was going through a backticked pattern and found another one inside it:",
    "You cannot put a var or val pattern inside another one."
)

val invalidPattern = PalmError(
    "INVALID PATTERN",
    "I was expecting a pattern, but instead got:",
    ""
)

fun unexpectedSymbol(symbol: String) = PalmError(
    "UNEXPECTED SYMBOL",
    "I was expecting a $symbol, but instead got:",
    ""
)

val missingTypeReturnType = PalmError(
    "MISSING RETURN TYPE",
    "I was going through a function type, but couldn't find a return type:",
    "Add an error and then the return type here."
)

fun keywordDecName(keyword: String) = PalmError(
    "INVALID VARIABLE NAME",
    "I was expecting a variable name, but instead got the $keyword keyword:",
    "Variable names must not be keyword."
)

val missingDeclarationName = PalmError(
    "MISSING VARIABLE NAME",
    "I was expecting a variable name, but couldn't find one:",
    ""
)

val missingInInFor = PalmError(
    "MISSING IN",
    "I was going through a for loop and was expecting in next, but instead got:",
    "Add in here."
)

val missingElseInGuard = PalmError(
    "MISSING ELSE",
    "I was going through a guard statement and was expecting else, but instead got:",
    "You need to put an else after the expression."
)

val missingArrowOnBranch = PalmError(
    "MISSING ARROW",
    "I was going through a when branch and was expecting an arrow next, but instead got:",
    "Add an -> here."
)

val missingColonInMap = PalmError(
    "MISSING COLON",
    "I was going through a map literal and was expecting a colon, but instead got:",
    "Add a colon here."
)

val invalidLambdaArguments = PalmError(
    "INVALID LAMBDA ARGUMENTS",
    "",
    ""
)

val postfixOperationOnType = PalmError(
    "POSTFIX OPERATION ON TYPE",
    "I saw a postfix operation after a type:",
    "You should use parentheses, so I know what you wanted the postfix operation to be applied on."
)

private fun aOrAn(nextStart: Char) = when (nextStart) {
    'a', 'e', 'i', 'o', 'u' -> "an"
    else -> "a"
}

private fun missing(thing: String): PalmError {
    val particle = aOrAn(thing.first())
    return PalmError(
        "MISSING ${thing.toUpperCase()}",
        "I was expecting $particle $thing here, but instead got:",
        "Add $particle $thing here."
    )
}

val missingIdentifier = missing("identifier")

val unknownParamModifier = PalmError(
    "UNKNOWN PARAMETER MODIFIER",
    "I was going through the parameters of a function and found a parameter modifier I don't recognize:",
    "If you wanted that to be a name of a parameter you need to put a colon and then its type after it."
)

val missingSymbol = missing("symbol")

val missingChar = missing("char")

val missingString = missing("string")

val missingNumber = missing("number")

val missingType = missing("type")

val missingTypeAnn = missing("type annotation")

val missingPattern = missing("pattern")

val missingCondition = missing("condition")

val missingExpression = missing("expression")

val missingLambda = missing("lambda")

val missingWildcard = missing("wildcard")

val notEOF = PalmError(
    "NOT END OF LINE",
    "I got to:",
    "I was expecting to see the end of the file next"
)

val unclosedIdentifier = PalmError(
    "UNCLOSED BACKTICKED IDENTIFIER",
    "I was going through a backticked identifier and have reached the end the file:",
    "You're missing a second backtick."
)

val emptyIdent = PalmError(
    "EMPTY BACKTICKED IDENTIFIER",
    "I was going through a backticked identifier, but didn't find anything inside it",
    "Put something in between the backticks."
)

val tooLongUnicode = PalmError(
    "INVALID UNICODE",
    "I was going through an escaped unicode, but the one given was too long:",
    "The unicode needs to be up to 8 characters long."
)

val emptyUnicode = PalmError(
    "EMPTY UNICODE",
    "I was expecting a unicode, but didn't find one:",
    "The unicode needs to be between 1 and 8 characters long."
)

val invalidInterpolation = PalmError(
    "INVALID STRING INTERPOLATION",
    "I was expecting either an identifier or a this, but instead got:",
    ""
)

fun unknownPostfixOp(symbol: String) = PalmError(
    "UNKNOWN POSTFIX OPERATOR",
    "I saw $symbol being used as a postfix operator, but there isn't an operation that that would represent:",
    "Add a space after it so it's treated as an infix operator"
)

fun invalidInfixOp(symbol: String) = PalmError(
    "INVALID INFIX OPERATOR",
    "I saw the infix operator $symbol not being used as an infix operator:",
    "Infix operators needs to have either a space or a term on both side."
)

fun invalidPrefixOp(symbol: String) = PalmError(
    "INVALID PREFIX OPERATOR",
    "I saw the prefix operator $symbol not being used as an prefix operator:",
    "Prefix operators need to have a term directly after them and whitespace before them."
)

fun invalidPostfixOp(symbol: String) = PalmError(
    "INVALID POSTFIX OPERATOR",
    "I saw the postfix operator $symbol not being used as an postfix operator:",
    "Postfix operators need to have a term directly before them and whitespace after them."
)

fun unsupportedOperator(provided: String, instead: String) = PalmError(
    "UNSUPPORTED OPERATOR",
    "I saw $provided, which is not a supported operator:",
    "Use `$instead` instead."
)

fun unexpectedCharacter(char: Char) = PalmError(
    "UNEXPECTED CHARACTER",
    "I found '$char' (U${char.toInt().toString(16)}):",
    "That character has no use in Palm code."
)

fun unexpectedConfusableCharacter(given: Char, givenName: String, proper: Char, properName: String) = PalmError(
    "UNEXPECTED CHARACTER",
    "I found a '$given' ($givenName):",
    "Did you mean to use a '$proper' ($properName?"
)

val missingLabelName = PalmError(
    "MISSING LABEL NAME",
    "I saw an @, but didn't see its name afterwards:",
    "The name of the label needs to go directly after the @.."
)

val unclosedMultilineComment = PalmError(
    "UNCLOSED MULTILINE COMMENT",
    "I was going through a multiline comment and have reached the end of the file:",
    "Closed it via *\\."
)

val invalidAccessOperand = PalmError(
    "INVALID ACCESS OPERAND",
    "I saw a dot and was expecting an identifier or integer next, but instead got:",
    "You use identifiers to access named members and digits to access unnamed tuple type elements."
)

val invalidFunctionReference = PalmError(
    "INVALID FUNCTION REFERENCE",
    "I saw a double colon and was expecting an identifier next, but instead got:",
    "Double colons are used to reference functions, such that the function name goes after it."
)

val emptyGetBody = PalmError(
    "EMPTY GET",
    "I saw a postfix get, but I couldn't find any parameters:",
    "A get needs to have at least one parameter."
)

fun doubleModifier(about: String) = PalmError(
    "DOUBLE ${about.capitalize()}",
    "I saw ${aOrAn(about.first())} $about modifier on this function parameter, but its $about was already set:",
    "Remove either this or the first $about modifier."
)

val doubleInlineHandling = doubleModifier("inline handling")
val doublePrivacy = doubleModifier("privacy")

val missingFunParams = PalmError(
    "MISSING FUNCTION PARAMETERS",
    "I was expecting to see the start of the function parameters, but instead got:",
    "Functions need to have parameters inside parentheses after their name."
)

val missingConstructorParams = PalmError(
    "MISSING CONSTRUCTOR PARAMETERS",
    "I was expecting to see the start of the contructor parameters, but instead got:",
    "Constructors need to have parameters inside parentheses after the kwyword."
)