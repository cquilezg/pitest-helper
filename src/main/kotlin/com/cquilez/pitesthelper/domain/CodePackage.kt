package com.cquilez.pitesthelper.domain

import java.nio.file.Path

data class CodePackage(
    override val path: Path,
    override val qualifiedName: String,
    override val sourceFolder: SourceFolder
) : CodeElement(path, qualifiedName, sourceFolder) {
    override fun toTargetString(): String = "$qualifiedName.*"
}


