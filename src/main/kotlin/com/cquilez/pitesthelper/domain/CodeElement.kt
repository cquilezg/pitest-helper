package com.cquilez.pitesthelper.domain

import java.nio.file.Path

abstract class CodeElement(
    open val path: Path,
    open val qualifiedName: String,
    open val sourceFolder: SourceFolder
)


