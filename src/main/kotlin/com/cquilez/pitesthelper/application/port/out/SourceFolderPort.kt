package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.CodeClass
import com.cquilez.pitesthelper.domain.CodeElement
import com.cquilez.pitesthelper.domain.CodePackage
import com.cquilez.pitesthelper.domain.SourceFolder

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

