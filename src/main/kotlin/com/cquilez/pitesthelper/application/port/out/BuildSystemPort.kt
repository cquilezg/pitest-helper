package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.BuildUnit
import com.intellij.openapi.project.Project

interface BuildSystemPort {
    fun getBuildSystem(): BuildSystem
    fun executeCommand(project: Project, buildUnit: BuildUnit, goalsOrTasks: List<String>, propertiesArg: String)
}