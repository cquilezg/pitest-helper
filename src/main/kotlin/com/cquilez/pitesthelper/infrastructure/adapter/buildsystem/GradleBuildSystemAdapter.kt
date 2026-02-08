package com.cquilez.pitesthelper.infrastructure.adapter.buildsystem

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.BuildUnit
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.action.GradleExecuteTaskAction

class GradleBuildSystemAdapter : AbstractBuildSystemAdapter() {

    override fun getBuildSystem(): BuildSystem = BuildSystem.GRADLE

    override fun executeCommand(
        project: Project,
        buildUnit: BuildUnit,
        goalsOrTasks: List<String>,
        propertiesArg: String
    ) {
        val command = buildList {
            addAll(goalsOrTasks)
            if (propertiesArg.isNotBlank()) add(propertiesArg)
        }.joinToString(" ")
        project.basePath?.let { GradleExecuteTaskAction.runGradle(project, null, it, command) }
    }
}

