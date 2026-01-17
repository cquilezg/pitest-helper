package com.cquilez.pitesthelper.domain

import java.nio.file.Path

data class CodeClass(
    override val path: Path,
    override val sourceFolder: SourceFolder,
    override val qualifiedName: String,
    val simpleName: String
) : CodeElement(path, qualifiedName, sourceFolder)


