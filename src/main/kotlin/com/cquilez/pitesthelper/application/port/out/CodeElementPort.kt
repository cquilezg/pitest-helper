package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.model.CodeElement
import java.nio.file.Path

interface CodeElementPort {
    fun getCodeElements(nodes: List<Path>): Pair<List<CodeElement>, List<String>>
    fun removeNestedElements(codeElements: List<CodeElement>): List<CodeElement>
}

