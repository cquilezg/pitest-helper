package com.cquilez.pitesthelper.domain.model

import com.cquilez.pitesthelper.domain.BuildUnit
import java.nio.file.Path

/**
 * Represents a source folder within a build unit.
 * 
 * @property path File system path to the source folder
 * @property codeType Whether this contains production or test code
 * @property buildUnit The build unit (module) that contains this source folder
 */
data class SourceFolder(
    val path: Path,
    val codeType: CodeType,
    val buildUnit: BuildUnit
)

