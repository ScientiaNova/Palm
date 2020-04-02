package com.scientianova.palm.registry

fun <F, S> to(first: F, second: S) = first to second

fun downTo(from: Int, to: Byte) = from downTo to
fun downTo(from: Long, to: Byte) = from downTo to
fun downTo(from: Byte, to: Byte) = from downTo to
fun downTo(from: Short, to: Byte) = from downTo to
fun downTo(from: Char, to: Char) = from downTo to
fun downTo(from: Int, to: Int) = from downTo to
fun downTo(from: Long, to: Int) = from downTo to
fun downTo(from: Byte, to: Int) = from downTo to
fun downTo(from: Short, to: Int) = from downTo to
fun downTo(from: Int, to: Long) = from downTo to
fun downTo(from: Long, to: Long) = from downTo to
fun downTo(from: Byte, to: Long) = from downTo to
fun downTo(from: Short, to: Long) = from downTo to
fun downTo(from: Int, to: Short) = from downTo to
fun downTo(from: Long, to: Short) = from downTo to
fun downTo(from: Byte, to: Short) = from downTo to
fun downTo(from: Short, to: Short) = from downTo to

fun until(from: Int, to: Byte) = from until to
fun until(from: Long, to: Byte) = from until to
fun until(from: Byte, to: Byte) = from until to
fun until(from: Short, to: Byte) = from until to
fun until(from: Char, to: Char) = from until to
fun until(from: Int, to: Int) = from until to
fun until(from: Long, to: Int) = from until to
fun until(from: Byte, to: Int) = from until to
fun until(from: Short, to: Int) = from until to
fun until(from: Int, to: Long) = from until to
fun until(from: Long, to: Long) = from until to
fun until(from: Byte, to: Long) = from until to
fun until(from: Short, to: Long) = from until to
fun until(from: Int, to: Short) = from until to
fun until(from: Long, to: Short) = from until to
fun until(from: Byte, to: Short) = from until to
fun until(from: Short, to: Short) = from until to

fun step(progression: IntProgression, step: Int) = progression step step
fun step(progression: LongProgression, step: Long) = progression step step
fun step(progression: CharProgression, step: Int) = progression step step