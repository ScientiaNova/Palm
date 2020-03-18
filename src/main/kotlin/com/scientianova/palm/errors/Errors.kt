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

val LONE_SINGLE_QUOTE_ERROR = PalmError(
    "LONE SINGLE QUOTE",
    "I found a single quote by itself at the end of a line",
    """
Was this by mistake? Maybe you want to get the new line character? If it's the latter, use \n.
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
    "I went to the end of the line and couldn't find the end of thing multi-line string:",
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