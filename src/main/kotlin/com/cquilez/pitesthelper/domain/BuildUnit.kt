package com.cquilez.pitesthelper.domain

import com.cquilez.pitesthelper.domain.model.SourceFolder
import java.nio.file.Path

open class BuildUnit(
    val buildSystem: BuildSystem,
    open val buildPath: Path,
    val buildFileName: String,
    val sourceFolders: List<SourceFolder> = emptyList(),
    val buildUnits: List<BuildUnit> = emptyList()
)
