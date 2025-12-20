package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.model.CodeElement
import java.nio.file.Path

fun interface CodeElementPort {
    fun getCodeElements(nodes: List<Path>): Pair<List<CodeElement>, List<String>>
}

