package com.scientianova.palm.errors

open class PalmError(val name: String, val context: String, val help: String)

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
  \u<code> - character with with a given unicode code
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

val UNKNOWN_SYMBOL_ERROR = PalmError(
    "UNKNOWN SYMBOL",
    "I found a symbol that I don't recognize:",
    ""
)

val INVALID_INTERPOLATION_ERROR = PalmError(
    "INVALID INTERPOLATION",
    "I was expecting a curly bracket here to end the string interpolation, but instead got:",
    "Add a } here."
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

val MISSING_ELSE_BRANCH = PalmError(
    "UNCLOSED SQUARE BRACKET",
    "I was expecting a closed square bracket here, but instead got:",
    "Add a ] here."
)

val MISSING_COLON_IN_TERNARY_ERROR = PalmError(
    "MISSING COLON",
    "I was going through a ternary expression and was expecting a colon next, but instead got:",
    "Add a : here."
)

val MISSING_COLON_IN_MAP_ERROR = PalmError(
    "MISSING COLON",
    "I was going through a map and was expecting a colon next, but instead got:",
    "Add a : here."
)

val INVALID_EXPRESSION_ERROR = PalmError(
    "INVALID EXPRESSION",
    "I was expecting an expression, but instead got:",
    ""
)

val MISSING_EXPRESSION_ERROR = PalmError(
    "MISSING EXPRESSION",
    "I was expecting an expression, but couldn't find one",
    ""
)

val MISSING_CURLY_BRACKET_AFTER_WHEN_ERROR = PalmError(
    "MISSING CURLY_BRACKET",
    "I was saw a when and was expecting an open curly bracket next, but instead got:",
    "Add an { here"
)

val MISSING_CURLY_BRACKET_AFTER_WHERE_ERROR = PalmError(
    "MISSING CURLY_BRACKET",
    "I was saw a where and was expecting an open curly bracket next, but instead got:",
    "Add an { here"
)

val INVALID_PROPERTY_NAME_ERROR = PalmError(
    "INVALID PROPERTY NAME",
    "I was expecting the name of a property next, but instead got:",
    "Property names have to start with a letter and can contain digits and underscores."
)

val INVALID_VARIABLE_NAME_IN_COMPREHENSION_ERROR = PalmError(
    "INVALID VARIABLE NAME",
    "I was going through list comprehension and was expecting a variable name next, but instead got:",
    "Variable names have to start with a letter and can contain digits and underscores."
)

val MISSING_IN_ERROR = PalmError(
    "MISSING IN",
    "I was going through list comprehension and was expecting in next, but instead got:",
    "Add 'in' here."
)

val UNCLOSED_OBJECT_ERROR = PalmError(
    "UNCLOSED OBJECT",
    "I was at he end of an object and was expecting a closed curly bracket next, but instead got:",
    "Add a } here."
)

val INVALID_KEY_NAME_ERROR = PalmError(
    "INVALID KEY NAME",
    "I was expecting the name of an object key, but instead got:",
    """
Raw key names have to start with a letter and can contain digits and underscores. 

Note: If you want to use additional characters, you can use a string (without interpolation) as a key name i.e. put the name in quotes.
""".trimIndent()
)

val MISSING_COLON_OR_EQUALS_IN_OBJECT_ERROR = PalmError(
    "MISSING COLON OR EQUALS",
    "I was going through an object and was expecting either and equals sign or a colon next, but instead got:",
    "Add a = or : here."
)

val MISSING_COLON_OR_EQUALS_IN_WHERE_ERROR = PalmError(
    "MISSING COLON OR EQUALS",
    "I was going through a where expression and was expecting either and equals sign or a colon next, but instead got:",
    "Add a = or : here."
)

val INVALID_CONSTANT_NAME_ERROR = PalmError(
    "INVALID CONSTANT NAME",
    "I was going through a where expression and was expecting the name of a constant next, but instead got:",
    "Names of constants have to start with a letter and can contain digits and underscores."
)

val UNCLOSED_WHERE_ERROR = PalmError(
    "UNCLOSED WHERE",
    "I was at he end of a where expression and was expecting a closed curly bracket next, but instead got:",
    "Add a } here."
)

val MISSING_ARROW_ERROR = PalmError(
    "MISSING ERROR",
    "I was going through a when expression and was expecting an arrow next, but instead got:",
    "Add an -> here."
)

val UNCLOSED_WHEN_ERROR = PalmError(
    "UNCLOSED WHEN",
    "I was at he end of a when expression and was expecting a closed curly bracket next, but instead got:",
    "Add a } here."
)

val INVALID_TYPE_NAME = PalmError(
    "INVALID TYPE NAME",
    "I was expecting the name of a type, but instead got:",
    "Type names have to start with a letter and can contain digits and underscores. The path of the type is separated via dots."
)

val UNKNOWN_TYPE_ERROR = PalmError(
    "UNKNOWN TYPE", "I couldn't find a registered type with the following name and path:", ""
)