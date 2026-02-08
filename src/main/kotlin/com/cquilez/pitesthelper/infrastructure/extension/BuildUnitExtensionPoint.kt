package com.cquilez.pitesthelper.infrastructure.extension

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.BuildUnit
import com.intellij.openapi.project.Project

interface BuildUnitExtensionPoint {
    fun getBuildSystem(): BuildSystem
    fun scanBuildUnits(project: Project): List<BuildUnit>
}