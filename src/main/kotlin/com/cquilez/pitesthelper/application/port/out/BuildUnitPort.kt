package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.BuildUnit
import java.nio.file.Path

interface BuildUnitPort {
    fun cleanScanBuildUnits(): List<BuildUnit>

    fun isPathBuildUnit(path: Path): Boolean

}