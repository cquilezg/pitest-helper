package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.CodeElement
import java.nio.file.Path

interface CodeElementPort {
    fun findCodeElements(nodes: List<Path>): Pair<List<CodeElement>, List<String>>
}

