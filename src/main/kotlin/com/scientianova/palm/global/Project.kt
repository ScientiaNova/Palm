package com.scientianova.palm.global

import com.scientianova.palm.queries.PackageId
import com.scientianova.palm.util.HashBasedTree

object Project {
    val structure = HashBasedTree<String, PackageId>()
}