package com.scientianovateam.palm.tokenizer

class StringTraverser(private val code: String) {
    private var index = -1;
    fun peek() = code.getOrNull(index)
    fun pop() = code.getOrNull(++index).also {
        if (lastRow != row) ++lastRow
        if (it == '\n') ++row
    }

    var row: Int = 1
        private set
    var lastRow = 1
        private set
}