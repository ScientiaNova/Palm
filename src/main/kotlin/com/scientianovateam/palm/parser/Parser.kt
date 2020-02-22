package com.scientianovateam.palm.parser

import com.scientianovateam.palm.flip
import com.scientianovateam.palm.tokenizer.tokenize

fun parse(code: String) {
    val stack = tokenize(code).flip()
}