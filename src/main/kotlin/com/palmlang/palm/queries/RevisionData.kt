package com.palmlang.palm.queries

import java.util.concurrent.atomic.AtomicInteger

@JvmInline
value class Revision internal constructor(val id: Int)

object RevisionData {
    private val atomic = AtomicInteger()
    val current get() = Revision(atomic.get())
    internal val incremented get() = Revision(atomic.getAndIncrement())
}