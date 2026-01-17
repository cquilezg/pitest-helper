package com.cquilez.pitesthelper.domain

import java.nio.file.Path

open class BuildUnit(
    val name: String,
    val buildSystem: BuildSystem,
    open val buildPath: Path,
    val sourceFolders: List<SourceFolder> = emptyList(),
    val buildUnits: List<BuildUnit> = emptyList()
)
