package com.scientianova.palm.queries

import java.util.concurrent.atomic.AtomicInteger

class Revision

object RevisionData {
    private val atomic = AtomicInteger()
    val current get() = atomic.get()
    internal val incremented get() = atomic.getAndIncrement()
}