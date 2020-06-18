package com.scientianova.palm.errors

import com.scientianova.palm.util.StringArea
import com.scientianova.palm.util.StringPos

class PalmError(val name: String, val context: String, val help: String)

infix fun PalmError.throwAt(area: StringArea): Nothing = throw UncaughtParserException(this, area)
infix fun PalmError.throwAt(pos: StringPos): Nothing = throwAt(pos..pos)

val MISSING_SINGLE_QUOTE_ERROR = PalmError(
    "MISSING SINGLE QUOTE",
    "I was expecting a single quote to closed the char literal",
    """
Add a single quote here.

Note: String literals are created with double quotes and char literals are created with single quotes. That way I can better know what you want to use.
    """.trimIndent()
)

val MISSING_SINGLE_QUOTE_ON_QUOTE_ERROR = PalmError(
    "MISSING SINGLE QUOTE",
    "I was expecting a single quote to closed the char literal",
    """
Add a single quote here.

Note: Backslashes are used for escape characters. If you want to get the backslash character you need to use \\.
    """.trimIndent()
)

val LONE_SINGLE_QUOTE_ERROR = PalmError(
    "LONE SINGLE QUOTE",
    "I found a single quote by itself at the end of a line",
    """
Was this by mistake? Maybe you want to get the new line character? If it's the latter, use \n.
    """.trimIndent()
)

val MALFORMED_TAB_ERROR = PalmError(
    "MALFORMED TAB",
    "I found a lot of spaces in between two single quotes:",
    """
This is most likely caused by your IDE automatically converting tabs into spaces. To say safe, use \t instead.
    """.trimIndent()
)

val INVALID_ESCAPE_CHARACTER_ERROR = PalmError(
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

val GREEK_QUESTION_MARK_ERROR = PalmError(
    "GREEK QUESTION MARK",
    "I found a greek question mark:",
    """
Greek question marks (U+037E) look like semicolons (U+003B), but they aren't.
    """.trimIndent()
)

val UNCLOSED_INTERPOLATED_EXPRESSION_ERROR = PalmError(
    "UNCLOSED INTERPOLATED EXPRESSION",
    "I couldn't find the end of an interpolated expression:",
    """
Closed putting a } somewhere.

Note: When you have a $ and a { after it, that's the beginning of an interpolated expression.
    """.trimIndent()
)

val MISSING_DOUBLE_QUOTE_ERROR = PalmError(
    "MISSING DOUBLE QUOTE",
    "I went to the end of this line and couldn't find the end of a single-line string:",
    """
Add a " here.

Note: If you want to use a multi-line string, you need to enclose the string in triple double quotes.
    """.trimIndent()
)

val UNCLOSED_MULTILINE_STRING = PalmError(
    "UNCLOSED MULTI_LINE STRING",
    "I couldn't find the end of this multi-line string:",
    "Closed it off using triple double quotes (\"\"\")."
)

val UNCLOSED_PARENTHESIS_ERROR = PalmError(
    "UNCLOSED PARENTHESIS",
    "I was expecting a closed parenthesis here, but instead got:",
    "Add a ) here."
)

val UNCLOSED_SQUARE_BRACKET_ERROR = PalmError(
    "UNCLOSED SQUARE BRACKET",
    "I was expecting a closed square bracket here, but instead got:",
    "Add a ] here."
)


val INVALID_EXPRESSION_ERROR = PalmError(
    "INVALID EXPRESSION",
    "I was expecting an expression, but instead got:",
    ""
)

val MISSING_BRACKET_IN_UNICODE_ERROR = PalmError(
    "MISSING CURLY BRACKET",
    "I saw a \\u and was expecting an open curly bracket next, but instead got:",
    "Put an { here."
)

val INVALID_EXPONENT_ERROR = PalmError(
    "INVALID EXPONENT",
    "I was going through a decimal literal with scientific notation and was expecting a digit next, but instead got:",
    ""
)

val INVALID_BINARY_LITERAL_ERROR = PalmError(
    "INVALID BINARY LITERAL",
    "I was going through a binary number literal and got:",
    "Binary number literals can only contain the digits 0 and 1."
)

val INVALID_DECIMAL_LITERAL_ERROR = PalmError(
    "INVALID DECIMAL LITERAL",
    "I was going through a decimal number literal and got:",
    "Decimal number literals can only contain the digits 0-9."
)

val INVALID_HEX_LITERAL_ERROR = PalmError(
    "INVALID HEX LITERAL",
    "I was going through a hexidecimal number literal and got:",
    "Hexidecimal number literals can only contain the digits 0-9, a-f and A-F."
)

val MISSING_CURLY_BRACKET_AFTER_WHEN_ERROR = PalmError(
    "MISSING CURLY BRACKET",
    "I was saw a when and was expecting an open curly bracket next, but instead got:",
    "Add an { here"
)

val MISSING_ARROW_ERROR = PalmError(
    "MISSING ERROR",
    "I was going through a when expression and was expecting an arrow next, but instead got:",
    "Add an -> here."
)

val INVALID_TYPE_NAME_ERROR = PalmError(
    "INVALID TYPE NAME",
    "I was expecting the name of a type, but instead got:",
    "Type names have to start with a letter and can contain digits and underscores. The path of the type is separated via double colons."
)

val INVALID_PATH_ERROR = PalmError(
    "INVALID PATH",
    "I thought I was going through a path and found:",
    "Paths can only contain identifiers separated with dots."
)

val INVALID_ALIAS_ERROR = PalmError(
    "INVALID ALIAS",
    "I was expecting an alias, but instead got:",
    "Aliases have to start with a letter and can contain digits and underscores."
)

val INVALID_PARAMETER_ERROR = PalmError(
    "INVALID PARAMETER",
    "I was going through the parameters of a function and found:",
    "You can only use names, wildcard or tuples holding them as parameters."
)

val INVALID_IMPORT_ERROR = PalmError(
    "INVALID IMPORT",
    "I was going through an import statement and found:",
    "Imports can only contain paths, that could end with ... or an operator, or could have an alias separated via 'as'."
)

val UNCLOSED_IMPORT_GROUP_ERROR = PalmError(
    "UNCLOSED IMPORT GROUP",
    "I was going through an import group and instead of a closed curly bracket found:",
    "Put a } here."
)

val MISSING_EQUALS_ERROR = PalmError(
    "MISSING EQUALS",
    "I was going through an assignment and was expecting an equals sign, but instead got:",
    "Put a = here."
)

val EMPTY_RECORD_ERROR = PalmError(
    "EMPTY RECORD",
    "I was going through a record declaration and saw it was amply:",
    "Records have to have at least one property."
)

val INVALID_PROPERTY_NAME_ERROR = PalmError(
    "INVALID PROPERTY NAME",
    "I was going through a record declaration and found an invalid property name:",
    "Properties have to start with a letter and can contain digits and underscores.."
)

val MISSING_COLON_IN_PROPERTY_ERROR = PalmError(
    "MISSING COLON",
    "I was going through a record property and was expecting a colon next, but instead got:",
    "Add a : here."
)

val MISSING_PAREN_AFTER_RECORD_ERROR = PalmError(
    "MISSING PARENTHESES",
    "I was going through a record declaration and was expecting an open parenthesis, but instead got:",
    "Add an ( here."
)

val MISSING_BRACKET_AFTER_ENUM_ERROR = PalmError(
    "MISSING CURLY BRACKET",
    "I was going through an enum declaration and was expecting an open curly bracket, but instead got:",
    "Put an { here."
)

val INVALID_ENUM_CASE_NAME_ERROR = PalmError(
    "INVALID ENUM CASE NAME",
    "I was going through an enum case and found an invalid case name name:",
    "Enum case names have to start with a letter and can contain digits and underscores."
)

val UNCLOSED_ENUM_ERROR = PalmError(
    "UNCLOSED ENUM",
    "I was going through an enum declaration and was expecting a closed curly bracket, but instead got:",
    "Add a } here."
)

val INVALID_GENERIC_VARIABLE_ERROR = PalmError(
    "INVALID GENERIC VARIABLE",
    "I was going through a generic pool and found:",
    "Generic variables have to start with a lowercase letter and can contain digits and underscores."
)

val INVALID_FIXITY_ERROR = PalmError(
    "INVALID FIXITY",
    "I was expecting an operator fixity, but got:",
    "Operators can only be prefix, postfix or infix."
)

val INVALID_PRECEDENCE = PalmError(
    "INVALID PRECEDENCE",
    "I was going through an infix operator declaration and found a precedence value that was out of bounds:",
    "An operator's precedence has to be a value between 0 and 15, both inclusive."
)

val INVALID_DESTRUCTURED_DECLARATION_ERROR = PalmError(
    "INVALID DESTRUCTURED DECLARATION",
    "I was going through a destructured declaration and got:",
    "A destructured declaration can only contain identifiers, wildcards or tuples of them."
)

val UNKNOWNN_DECLARATION_ERROR = PalmError(
    "UNKNOWN DECLARATION",
    "I was expecting a declaration, but what I found didn't match any of the possible ones:",
    ""
)

val INVALID_FUNCTION_NAME = PalmError(
    "INVALID FUNCTION NAME",
    "I was going through a function declaration and got:",
    "Function names have to start with a lowercase letter and can contain digits and underscores."
)

val MISSING_BRACKET_AFTER_CLASS_ERROR = PalmError(
    "MISSING CURLY BRACKET",
    "I was going through a type class declaration and was expecting an open curly bracket, but instead got:",
    "Put an { here."
)

val MISSING_BRACKET_AFTER_IMPL_ERROR = PalmError(
    "MISSING CURLY BRACKET",
    "I was going through a type class implementation and was expecting an open curly bracket, but instead got:",
    "Put an { here."
)

val MISSING_EXPRESSION_ERROR = PalmError(
    "MISSING EXPRESSION",
    "I was expecting an expression, but I seem to have reached the end of the file:",
    ""
)

val UNCLOSED_CHAR_LITERAL_ERROR = PalmError(
    "UNCLOSED CHAR LITERAL",
    "I was going through a char literal, but have reached the end of the file without finding its end:",
    "Put a ' here."
)