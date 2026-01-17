package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.BuildUnit
import com.intellij.openapi.project.Project

interface BuildUnitServicePort {
    fun getBuildSystem(): BuildSystem
    fun scanBuildUnits(project: Project): List<BuildUnit>
}

