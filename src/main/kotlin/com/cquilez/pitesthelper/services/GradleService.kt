package com.cquilez.pitesthelper.services

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.action.GradleExecuteTaskAction

object GradleService {

    fun runCommand(project: Project, command: String) {
        GradleExecuteTaskAction.runGradle(project, null, project.basePath!!, command)
    }
}