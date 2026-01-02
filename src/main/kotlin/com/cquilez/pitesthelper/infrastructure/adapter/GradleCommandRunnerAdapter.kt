package com.cquilez.pitesthelper.infrastructure.adapter

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.action.GradleExecuteTaskAction

@Service(Service.Level.PROJECT)
class GradleCommandRunnerAdapter(private val project: Project) {

    fun runGradleCommand(command: String) {
        GradleExecuteTaskAction.runGradle(project, null, project.basePath!!, command)
    }
}


