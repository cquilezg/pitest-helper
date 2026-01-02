package com.cquilez.pitesthelper.domain.model

import java.nio.file.Path

/**
 * Represents a source folder within a build unit.
 * 
 * @property path File system path to the source folder
 * @property codeType Whether this contains production or test code
 */
data class SourceFolder(
    val path: Path,
    val codeType: CodeType
)

