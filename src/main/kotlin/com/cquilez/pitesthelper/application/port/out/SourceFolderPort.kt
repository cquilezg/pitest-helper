package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.model.CodeClass
import com.cquilez.pitesthelper.domain.model.CodeElement
import com.cquilez.pitesthelper.domain.model.CodePackage
import com.cquilez.pitesthelper.domain.model.SourceFolder

interface SourceFolderPort {
    fun findCorrespondingPackage(
        pkg: CodePackage,
        oppositeSourceFolder: SourceFolder
    ): Pair<CodeElement?, String?>

    fun findCorrespondingClass(
        cls: CodeClass,
        oppositeSourceFolder: SourceFolder
    ): Pair<CodeElement?, String?>
}

