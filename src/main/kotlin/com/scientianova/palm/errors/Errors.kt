package com.scientianova.palm.errors

data class PalmError(val name: String, val context: String, val help: String)

val missingSingleQuoteError = PalmError(
    "MISSING SINGLE QUOTE",
    "I was expecting a single quote to closed the char literal",
    """
Add a single quote here.

Note: String literals are created with double quotes and char literals are created with single quotes. That way I can better know what you want to use.
    """.trimIndent()
)

val missingSingleQuoteOnQuoteError = PalmError(
    "MISSING SINGLE QUOTE",
    "I was expecting a single quote to closed the char literal",
    """
Add a single quote here.

Note: Backslashes are used for escape characters. If you want to get the backslash character you need to use \\.
    """.trimIndent()
)

val loneSingleQuoteError = PalmError(
    "LONE SINGLE QUOTE",
    "I found a single quote by itself at the end of a line",
    """
Was this by mistake? Maybe you want to get the new line character? If it's the latter, use \n.
    """.trimIndent()
)

val malformedTabError = PalmError(
    "MALFORMED TAB",
    "I found a lot of spaces in between two single quotes:",
    """
This is most likely caused by your IDE automatically converting tabs into spaces. To say safe, use \t instead.
    """.trimIndent()
)

val unclosedEscapeCharacterError = PalmError(
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

val missingDoubleQuoteError = PalmError(
    "MISSING DOUBLE QUOTE",
    "I went to the end of this line and couldn't find the end of a single-line string:",
    """
Add a " here.

Note: If you want to use a multi-line string, you need to enclose the string in triple double quotes.
    """.trimIndent()
)

val unclosedMultilineStringError = PalmError(
    "UNCLOSED MULTI_LINE STRING",
    "I couldn't find the end of this multi-line string:",
    "Closed it off using triple double quotes (\"\"\")."
)

val unclosedParenthesisError = PalmError(
    "UNCLOSED PARENTHESIS",
    "I was expecting a closed parenthesis here, but instead got:",
    "Add a ) here."
)

val unclosedSquareBacketError = PalmError(
    "UNCLOSED SQUARE BRACKET",
    "I was expecting a closed square bracket here, but instead got:",
    "Add a ] here."
)

val invalidExpressionError = PalmError(
    "INVALID EXPRESSION",
    "I was expecting an expression, but instead got:",
    ""
)

val missingBacketInUnicodeError = PalmError(
    "MISSING CURLY BRACKET",
    "I saw a \\u and was expecting an open curly bracket next, but instead got:",
    "Add an { here."
)

val invalidExponentError = PalmError(
    "INVALID EXPONENT",
    "I was going through a decimal literal with scientific notation and was expecting a digit next, but instead got:",
    ""
)

val invalidBinaryLiteralError = PalmError(
    "INVALID BINARY LITERAL",
    "I was going through a binary number literal and got:",
    "Binary number literals can only contain the digits 0 and 1."
)

val invalidDecimalLiteralError = PalmError(
    "INVALID DECIMAL LITERAL",
    "I was going through a decimal number literal and got:",
    "Decimal number literals can only contain the digits 0-9."
)

val invalidHexLiteralError = PalmError(
    "INVALID HEX LITERAL",
    "I was going through a hexidecimal number literal and got:",
    "Hexidecimal number literals can only contain the digits 0-9, a-f and A-F."
)

val missingScopeError = PalmError(
    "MISSING SCOPE",
    "I was expecting a scope next, but instead got:",
    "Start a scope here with an {."
)

val invalidImportError = PalmError(
    "INVALID IMPORT",
    "I was going through an import statement and found:",
    "Imports can only contain paths, that could end with ... or an operator, or could have an alias separated via 'as'."
)

val unclosedImportGroupError = PalmError(
    "UNCLOSED IMPORT GROUP",
    "I was going through an import group and instead of a closed curly bracket found:",
    "Add a } here."
)

val missingExpressionError = PalmError(
    "MISSING EXPRESSION",
    "I was expecting an expression, but I seem to have reached the end of the file:",
    ""
)

val unclosedCharLiteralError = PalmError(
    "UNCLOSED CHAR LITERAL",
    "I was going through a char literal, but have reached the end of the file without finding its end:",
    "Add a ' here."
)

val unclosedWhenError = PalmError(
    "UNCLOSED WHEN EXPRESSION",
    "I was going through a when expression and was expecting a closed curly bracket or expression separator, but instead got:",
    "Add a } here."
)

val invalidPrefixOperatorError = PalmError(
    "INVALID PREFIX OPERATOR",
    "I was expecting an infix operator, but instead got a prefix operator:",
    "Add a space here."
)

val missingExpressionSeparatorError = PalmError(
    "MISSING EXPRESSION SEPARATOR",
    "I saw 2 expression on the same line without a separator between them:",
    "Add a ; here."
)

val unclosedScopeError = PalmError(
    "UNCLOSED SCOPE",
    "I was going through a scope, but have reached the end of the file:",
    "Add a } here."
)

val invalidBacktickedIdentifier = PalmError(
    "INVALID BACKTICKED IDENTIFIER",
    "I was going through a backticked identifier and got:",
    "Backticked identifier can use any symbol but /, \\, ., ;, :, <, >, [, ], and line separators."
)

val invalidDoubleDeclarationError = PalmError(
    "DOUBLE DECLARATION PATTERN",
    "I was going through a backticked pattern and found another one inside it:",
    "You cannot put a var or val pattern inside another one."
)

val invalidPatternError = PalmError(
    "INVALID PATTERN",
    "I was expecting a pattern, but instead got:",
    ""
)


val emptyParenthesesOnExprError = PalmError(
    "EMPTY PARENTHESES",
    "I saw some parentheses and was expecting an expression, but didn't find one:",
    "Add an expression here."
)

fun unexpectedSymbolError(symbol: String) = PalmError(
    "UNEXPECTED SYMBOL",
    "I was expecting a $symbol, but instead got:",
    ""
)

val missingTypeReturnTypeError = PalmError(
    "MISSING RETURN TYPE",
    "I was going through a function type, but couldn't find a return type:",
    "Add an error and then the return type here."
)

fun keywordDecNameError(keyword: String) = PalmError(
    "INVALID VARIABLE NAME",
    "I was expecting a variable name, but instead got the $keyword keyword:",
    "Variable names must not be keyword."
)

val missingDeclarationNameError = PalmError(
    "MISSING VARIABLE NAME",
    "I was expecting a variable name, but couldn't find one:",
    ""
)

val missingInInForError = PalmError(
    "MISSING IN",
    "I was going through a for loop and was expecting in next, but instead got:",
    "Add in here."
)

val invalidLambdaArgumentsError = PalmError(
    "INVALID LAMBDA ARGUMENTS",
    "",
    ""
)

val invalidVariableDeclarationError = PalmError(
    "INVALID VARIABLE DECLARATION ERROR",
    "I was going through a variable declaration, but couldn't find a type or value after the type:",
    "When declaring a variable you need to specify the type, with :, or the value, with =."
)

val invalidTypePathError = PalmError(
    "INVALID PATH",
    "I was going through the path to type and was expecting an identifier next, but instead got:",
    ""
)

val postfixOperationOnTypeError = PalmError(
    "POSTFIX OPERATION ON TYPE",
    "I saw a postfix operation after a type:",
    "You should use parentheses, so I know what you wanted the postfix operation to be applied on."
)

fun aOrAn(nextStart: Char) = when (nextStart) {
    'a', 'e', 'i', 'o', 'u' -> "an"
    else -> "a"
}

fun missing(thing: String) = PalmError(
    "MISSING ${thing.toUpperCase()}",
    "I saw a expecting ${aOrAn(thing.first())} $thing here, but instead got:",
    "Add an $thing here."
)

val missingIdentifierError = missing("identifier")

val unknownParamModifierError = PalmError(
    "UNKNOWN PARAMETER MODIFIER",
    "I was going through the parameters of a function and found a parameter modifier I don't recognize:",
    "If you wanted that to be a name of a parameter you need to put a colon and then its type after it."
)

val missingSymbolError = missing("symbol")

val missingCharError = missing("char")

val missingStringError = missing("string")

val missingNumberError = missing("number")

val missingTypeError = missing("type")