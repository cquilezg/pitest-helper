package com.cquilez.pitesthelper.services

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.idea.maven.execution.MavenRunner
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.execution.MavenRunnerSettings

object MavenService {
    fun buildPitestArgs(targetClasses: String, targetTests: String) =
        "-DtargetClasses=$targetClasses -DtargetTests=$targetTests"

    fun runMavenCommand(project: Project, module: Module, goals: List<String>, vmOptions: String) {
        // Obtener la Tool Window de Maven
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow: ToolWindow? = toolWindowManager.getToolWindow("Maven")

        // Mostrar la Tool Window si está oculta
        if (toolWindow != null) {// Mostrar la Tool Window si está oculta
            if (!toolWindow.isVisible) {
                toolWindow.setAvailable(true, null)
                toolWindow.setType(toolWindow.type, null)
                toolWindow.activate(null)
            }
            // Obtener la vista de la Tool Window
            if (toolWindow.contentManager.contents.isNotEmpty()) {
                // TODO: comprobar que tiene un SDK asociado antes de ejecutar el goal
                if (module.rootManager.contentRoots.isNotEmpty()) {
                    val parameters = MavenRunnerParameters(
                        true, module.rootManager.contentRoots[0].path, "pom.xml", goals, emptyList()
                    )
                    val mavenRunner = MavenRunner(project)
                    val mavenRunnerSettings = MavenRunnerSettings()
                    mavenRunnerSettings.setVmOptions(vmOptions)
                    mavenRunner.run(parameters, mavenRunnerSettings) { }
                }
            }
        }
    }
}