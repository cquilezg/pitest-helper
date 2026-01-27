package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.CodeClass
import com.cquilez.pitesthelper.domain.CodeElement
import com.cquilez.pitesthelper.domain.CodePackage
import com.cquilez.pitesthelper.domain.SourceFolder

interface SourceFolderPort {
    fun findPackage(
        pkg: CodePackage,
        oppositeSourceFolder: SourceFolder
    ): Pair<CodeElement?, String?>

    fun findClass(
        cls: CodeClass,
        oppositeSourceFolder: SourceFolder
    ): Pair<CodeElement?, String?>
}

