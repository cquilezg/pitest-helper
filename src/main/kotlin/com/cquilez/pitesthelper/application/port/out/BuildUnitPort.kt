package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.BuildUnit
import com.cquilez.pitesthelper.domain.SourceFolder
import java.nio.file.Path

interface BuildUnitPort {
    fun cleanScanBuildUnits(): List<BuildUnit>

    fun getAllBuildUnits(): List<BuildUnit>

    fun isPathBuildUnit(path: Path): Boolean

    fun findBuildUnit(sourceFolder: SourceFolder): BuildUnit?

    fun findParent(sourceFolder: SourceFolder): BuildUnit?

    fun findParent(buildUnit: BuildUnit): BuildUnit?
}