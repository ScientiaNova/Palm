package com.scientianova.palm.lexer


fun Char.isIdentifierStart() = isLetter() || this == '_'
fun Char.isIdentifierPart() = isLetterOrDigit() || this == '_'

fun Char?.isBeforePostfix() = when (this) {
    null, ')', ']', '}', in identChars, '\"', '\'', '`' -> true
    else -> false
}

fun Char?.isAfterPrefix() = when (this) {
    '(', '[', '{', in identChars, '\"', '\'', '`' -> true
    else -> false
}

val charNames = mapOf(
    ' ' to "Space",
    '_' to "Underscore",
    ';' to "Colon",
    ';' to "Semicolon",
    ',' to "Comma",
    '.' to "Full Stop",
    '!' to "Exclamation Mark",
    '?' to "Question Mark",
    '+' to "Plus",
    '-' to "Minus/Hyphen",
    '*' to "Asterisk",
    '/' to "Dash",
    '&' to "Ampersand",
    '\\' to "Backslash",
    '=' to "Equals Sign",
    '(' to "Left Parenthesis",
    ')' to "Right Parenthesis",
    '{' to "Left Curly Bracket",
    '}' to "Right Curly Bracket",
    '\'' to "Single Quote",
    '\"' to "Double Quote",
)

val confusables = mapOf(
    ' ' to ("Line Separator" to ' '),
    ' ' to ("Paragraph Separator" to ' '),
    ' ' to ("Ogham Space Mark" to ' '),
    ' ' to ("En Quad" to ' '),
    ' ' to ("Em Quad" to ' '),
    ' ' to ("En Space" to ' '),
    ' ' to ("Em Space" to ' '),
    ' ' to ("Three-Per-Em Space" to ' '),
    ' ' to ("Four-Per-Em Space" to ' '),
    ' ' to ("Six-Per-Em Space" to ' '),
    ' ' to ("Punctuation Space" to ' '),
    ' ' to ("Thin Space" to ' '),
    ' ' to ("Hair Space" to ' '),
    ' ' to ("Medium Mathematical Space" to ' '),
    ' ' to ("No-Break Space" to ' '),
    ' ' to ("Figure Space" to ' '),
    ' ' to ("Narrow No-Break Space" to ' '),
    '　' to ("Ideographic Space" to ' '),

    'ߺ' to ("Nko Lajanyalan" to '_'),
    '﹍' to ("Dashed Low Line" to '_'),
    '﹎' to ("Centreline Low Line" to '_'),
    '﹏' to ("Wavy Low Line" to '_'),
    '＿' to ("Fullwidth Low Line" to '_'),

    '‐' to ("Hyphen" to '-'),
    '‑' to ("Non-Breaking Hyphen" to '-'),
    '‒' to ("Figure Dash" to '-'),
    '–' to ("En Dash" to '-'),
    '﹘' to ("Small Em Dash" to '-'),
    '⁃' to ("Hyphen Bullet" to '-'),
    '˗' to ("Modifier Letter Minus Sign" to '-'),
    '−' to ("Minus Sign" to '-'),
    '➖' to ("Heavy Minus Sign" to '-'),
    'Ⲻ' to ("Coptic Capital Letter Dialect-P Ni" to '-'),
    'ー' to ("Katakana-Hiragana Prolonged Sound Mark" to '-'),
    '－' to ("Fullwidth Hyphen-Minus" to '-'),
    '―' to ("Horizontal Bar" to '-'),
    '─' to ("Box Drawings Light Horizontal" to '-'),
    '━' to ("Box Drawings Heavy Horizontal" to '-'),
    '㇐' to ("CJK Stroke H" to '-'),
    'ꟷ' to ("Latin Epigraphic Letter Sideways I" to '-'),
    'ᅳ' to ("Hangul Jungseong Eu" to '-'),
    'ㅡ' to ("Hangul Letter Eu" to '-'),
    '一' to ("CJK Unified Ideograph-4E00" to '-'),
    '⼀' to ("Kangxi Radical One" to '-'),

    '٫' to ("Arabic Decimal Separator" to ','),
    '‚' to ("Single Low-9 Quotation Mark" to ','),
    '¸' to ("Cedilla" to ','),
    'ꓹ' to ("Lisu Letter Tone Na Po" to ','),
    '，' to ("Fullwidth Comma" to ','),

    ';' to ("Greek Question Mark" to ';'),
    '；' to ("Fullwidth Semicolon" to ';'),
    '︔' to ("Presentation Form For Vertical Semicolon" to ';'),

    'ः' to ("Devanagari Sign Visarga" to ':'),
    'ઃ' to ("Gujarati Sign Visarga" to ':'),
    '：' to ("Fullwidth Colon" to ':'),
    '։' to ("Armenian Full Stop" to ':'),
    '܃' to ("Syriac Supralinear Colon" to ':'),
    '܄' to ("Syriac Sublinear Colon" to ':'),
    '︰' to ("Presentation Form For Vertical Two Dot Leader" to ':'),
    '᠃' to ("Mongolian Full Stop" to ':'),
    '᠉' to ("Mongolian Manchu Full Stop" to ':'),
    '⁚' to ("Two Dot Punctuation" to ':'),
    '׃' to ("Hebrew Punctuation Sof Pasuq" to ':'),
    '˸' to ("Modifier Letter Raised Colon" to ':'),
    '꞉' to ("Modifier Letter Colon" to ':'),
    '∶' to ("Ratio" to ':'),
    'ː' to ("Modifier Letter Triangular Colon" to ':'),
    'ꓽ' to ("Lisu Letter Tone Mya Jeu" to ':'),
    '︓' to ("Presentation Form For Vertical Colon" to ':'),

    '！' to ("Fullwidth Exclamation Mark" to '!'),
    'ǃ' to ("Latin Letter Retroflex Click" to '!'),
    'ⵑ' to ("Tifinagh Letter Tuareg Yang" to '!'),
    '︕' to ("Presentation Form For Vertical Exclamation Mark" to '!'),

    'ʔ' to ("Latin Letter Glottal Stop" to '?'),
    'Ɂ' to ("Latin Capital Letter Glottal Stop" to '?'),
    'ॽ' to ("Devanagari Letter Glottal Stop" to '?'),
    'Ꭾ' to ("Cherokee Letter He" to '?'),
    'ꛫ' to ("Bamum Letter Ntuu" to '?'),
    '？' to ("Fullwidth Question Mark" to '?'),
    '︖' to ("Presentation Form For Vertical Question Mark" to '?'),

    0x1D16D.toChar() to ("Musical Symbol Combining Augmentation Dot" to '.'),
    '․' to ("One Dot Leader" to '.'),
    '۔' to ("Arabic Full Stop" to '.'),
    '܁' to ("Syriac Supralinear Full Stop" to '.'),
    '܂' to ("Syriac Sublinear Full Stop" to '.'),
    '꘎' to ("Vai Full Stop" to '.'),
    0x10A50.toChar() to ("Kharoshthi Punctuation Dot" to '.'),
    '٠' to ("Arabic-Indic Digit Zero" to '.'),
    '۰' to ("Extended Arabic-Indic Digit Zero" to '.'),
    'ꓸ' to ("Lisu Letter Tone Mya Ti" to '.'),
    '·' to ("Middle Dot" to '.'),
    '・' to ("Katakana Middle Dot" to '.'),
    '･' to ("Halfwidth Katakana Middle Dot" to '.'),
    '᛫' to ("Runic Single Punctuation" to '.'),
    '·' to ("Greek Ano Teleia" to '.'),
    '⸱' to ("Word Separator Middle Dot" to '.'),
    0x10101.toChar() to ("Aegean Word Separator Dot" to '.'),
    '•' to ("Bullet" to '.'),
    '‧' to ("Hyphenation Point" to '.'),
    '∙' to ("Bullet Operator" to '.'),
    '⋅' to ("Dot Operator" to '.'),
    'ꞏ' to ("Latin Letter Sinological Dot" to '.'),
    'ᐧ' to ("Canadian Syllabics Final Middle Dot" to '.'),
    'ᐧ' to ("Canadian Syllabics Final Middle Dot" to '.'),
    '．' to ("Fullwidth Full Stop" to '.'),
    '。' to ("Ideographic Full Stop" to '.'),
    '︒' to ("Presentation Form For Vertical Ideographic Full Stop" to '.'),

    '՝' to ("Armenian Comma" to '\''),
    '＇' to ("Fullwidth Apostrophe" to '\''),
    '‘' to ("Left Single Quotation Mark" to '\''),
    '’' to ("Right Single Quotation Mark" to '\''),
    '‛' to ("Single High-Reversed-9 Quotation Mark" to '\''),
    '′' to ("Prime" to '\''),
    '‵' to ("Reversed Prime" to '\''),
    '՚' to ("Armenian Apostrophe" to '\''),
    '׳' to ("Hebrew Punctuation Geresh" to '\''),
    '`' to ("Greek Varia" to '\''),
    '｀' to ("Fullwidth Grave Accent" to '\''),
    '΄' to ("Greek Tonos" to '\''),
    '´' to ("Greek Oxia" to '\''),
    '᾽' to ("Greek Koronis" to '\''),
    '᾿' to ("Greek Psili" to '\''),
    '῾' to ("Greek Dasia" to '\''),
    'ʹ' to ("Modifier Letter Prime" to '\''),
    'ʹ' to ("Greek Numeral Sign" to '\''),
    'ˊ' to ("Modifier Letter Acute Accent" to '\''),
    'ˋ' to ("Modifier Letter Grave Accent" to '\''),
    '˴' to ("Modifier Letter Middle Grave Accent" to '\''),
    'ʻ' to ("Modifier Letter Turned Comma" to '\''),
    'ʽ' to ("Modifier Letter Reversed Comma" to '\''),
    'ʼ' to ("Modifier Letter Apostrophe" to '\''),
    'ʾ' to ("Modifier Letter Right Half Ring" to '\''),
    'ꞌ' to ("Latin Small Letter Saltillo" to '\''),
    'י' to ("Hebrew Letter Yod" to '\''),
    'ߴ' to ("Nko High Tone Apostrophe" to '\''),
    'ߵ' to ("Nko Low Tone Apostrophe" to '\''),
    'ᑊ' to ("Canadian Syllabics West-Cree P" to '\''),
    'ᛌ' to ("Runic Letter Short-Twig-Sol S" to '\''),
    0x16F51.toChar() to ("Miao Sign Aspiration" to '\''),
    0x16F52.toChar() to ("Miao Sign Reformed Voicing" to '\''),

    '᳓' to ("Vedic Sign Nihshvasa" to '"'),
    '＂' to ("Fullwidth Quotation Mark" to '"'),
    '“' to ("Left Double Quotation Mark" to '"'),
    '”' to ("Right Double Quotation Mark" to '"'),
    '‟' to ("Double High-Reversed-9 Quotation Mark" to '"'),
    '″' to ("Double Prime" to '"'),
    '‶' to ("Reversed Double Prime" to '"'),
    '〃' to ("Ditto Mark" to '"'),
    '״' to ("Hebrew Punctuation Gershayim" to '"'),
    '˝' to ("Double Acute Accent" to '"'),
    'ʺ' to ("Modifier Letter Double Prime" to '"'),
    '˶' to ("Modifier Letter Middle Double Acute Accent" to '"'),
    '˵' to ("Modifier Letter Middle Double Grave Accent" to '"'),
    'ˮ' to ("Modifier Letter Double Apostrophe" to '"'),
    'ײ' to ("Hebrew Ligature Yiddish Double Yod" to '"'),
    '❞' to ("Heavy Double Comma Quotation Mark Ornament" to '"'),
    '❝' to ("Heavy Double Turned Comma Quotation Mark Ornament" to '"'),


    '［' to ("Fullwidth Left Square Bracket" to '['),
    '❲' to ("Light Left Tortoise Shell Bracket Ornament" to '['),
    '「' to ("Left Corner Bracket" to '['),
    '『' to ("Left White Corner Bracket" to '['),
    '【' to ("Left Black Lenticular Bracket" to '['),
    '〔' to ("Left Tortoise Shell Bracket" to '['),
    '〖' to ("Left White Lenticular Bracket" to '['),
    '〘' to ("Left White Tortoise Shell Bracket" to '['),
    '〚' to ("Left White Square Bracket" to '['),

    '］' to ("Fullwidth Right Square Bracket" to ']'),
    '❳' to ("Light Right Tortoise Shell Bracket Ornament" to ']'),
    '」' to ("Right Corner Bracket" to ']'),
    '』' to ("Right White Corner Bracket" to ']'),
    '】' to ("Right Black Lenticular Bracket" to ']'),
    '〕' to ("Right Tortoise Shell Bracket" to ']'),
    '〗' to ("Right White Lenticular Bracket" to ']'),
    '〙' to ("Right White Tortoise Shell Bracket" to ']'),
    '〛' to ("Right White Square Bracket" to ']'),

    '❴' to ("Medium Left Curly Bracket Ornament" to '{'),
    0x1D114.toChar() to ("Musical Symbol Brace" to '{'),
    '｛' to ("Fullwidth Left Curly Bracket" to '{'),

    '❵' to ("Medium Right Curly Bracket Ornament" to '}'),
    '｝' to ("Fullwidth Right Curly Bracket" to '}'),

    '⁎' to ("Low Asterisk" to '*'),
    '٭' to ("Arabic Five Pointed Star" to '*'),
    '∗' to ("Asterisk Operator" to '*'),
    0x1031F.toChar() to ("Old Italic Letter Ess" to '*'),
    '＊' to ("Fullwidth Asterisk" to '*'),

    '᜵' to ("Philippine Single Punctuation" to '/'),
    '⁁' to ("Caret Insertion Point" to '/'),
    '∕' to ("Division Slash" to '/'),
    '⁄' to ("Fraction Slash" to '/'),
    '╱' to ("Box Drawings Light Diagonal Upper Right To Lower Left" to '/'),
    '⟋' to ("Mathematical Rising Diagonal" to '/'),
    '⧸' to ("Big Solidus" to '/'),
    '㇓' to ("Cjk Stroke Sp" to '/'),
    '〳' to ("Vertical Kana Repeat Mark Upper Half" to '/'),
    'Ⳇ' to ("Coptic Capital Letter Old Coptic Esh" to '/'),
    '丿' to ("Cjk Unified Ideograph-4E3F" to '/'),
    '⼃' to ("Kangxi Radical Slash" to '/'),
    '／' to ("Fullwidth Solidus" to '/'),

    '＼' to ("Fullwidth Reverse Solidus" to '\\'),
    '﹨' to ("Small Reverse Solidus" to '\\'),
    '∖' to ("Set Minus" to '\\'),
    '⟍' to ("Mathematical Falling Diagonal" to '\\'),
    '⧵' to ("Reverse Solidus Operator" to '\\'),
    '⧹' to ("Big Reverse Solidus" to '\\'),
    '㇔' to ("Cjk Stroke D" to '\\'),
    '丶' to ("Cjk Unified Ideograph-4E36" to '\\'),
    '⼂' to ("Kangxi Radical Dot" to '\\'),
    '、' to ("Ideographic Comma" to '\\'),
    'ヽ' to ("Katakana Iteration Mark" to '\\'),

    'ꝸ' to ("Latin Small Letter Um" to '&'),
    '＆' to ("Fullwidth Ampersand" to '&'),

    '᛭' to ("Runic Cross Punctuation" to '+'),
    '➕' to ("Heavy Plus Sign" to '+'),
    0x1029B.toChar() to ("Lycian Letter H" to '+'),
    '﬩' to ("Hebrew Letter Alternative Plus Sign" to '+'),
    '＋' to ("Fullwidth Plus Sign" to '+'),

    '‹' to ("Single Left-Pointing Angle Quotation Mark" to '<'),
    '❮' to ("Heavy Left-Pointing Angle Quotation Mark Ornament" to '<'),
    '˂' to ("Modifier Letter Left Arrowhead" to '<'),
    0x1D236.toChar() to ("Greek Instrumental Symbol-40" to '<'),
    'ᐸ' to ("Canadian Syllabics Pa" to '<'),
    'ᚲ' to ("Runic Letter Kauna" to '<'),
    '❬' to ("Medium Left-Pointing Angle Bracket Ornament" to '<'),
    '⟨' to ("Mathematical Left Angle Bracket" to '<'),
    '〈' to ("Left-Pointing Angle Bracket" to '<'),
    '〈' to ("Left Angle Bracket" to '<'),
    '㇛' to ("CJK Stroke Pd" to '<'),
    'く' to ("Hiragana Letter Ku" to '<'),
    0x21FE8.toChar() to ("CJK Unified Ideograph-21FE8" to '<'),
    '《' to ("Left Double Angle Bracket" to '<'),
    '＜' to ("Fullwidth Less-Than Sign" to '<'),

    '᐀' to ("Canadian Syllabics Hyphen" to '='),
    '⹀' to ("Double Hyphen" to '='),
    '゠' to ("Katakana-Hiragana Double Hyphen" to '='),
    '꓿' to ("Lisu Punctuation Full Stop" to '='),
    '＝' to ("Fullwidth Equals Sign" to '='),

    '›' to ("Single Right-Pointing Angle Quotation Mark" to '>'),
    '❯' to ("Heavy Right-Pointing Angle Quotation Mark Ornament" to '>'),
    '˃' to ("Modifier Letter Right Arrowhead" to '>'),
    0x1D237.toChar() to ("Greek Instrumental Symbol-42" to '>'),
    'ᐳ' to ("Canadian Syllabics Po" to '>'),
    0x16F3F.toChar() to ("Miao Letter Archaic Zza" to '>'),
    '❭' to ("Medium Right-Pointing Angle Bracket Ornament" to '>'),
    '⟩' to ("Mathematical Right Angle Bracket" to '>'),
    '〉' to ("Right-Pointing Angle Bracket" to '>'),
    '〉' to ("Right Angle Bracket" to '>'),
    '》' to ("Right Double Angle Bracket" to '>'),
    '＞' to ("Fullwidth Greater-Than Sign" to '>'),
)