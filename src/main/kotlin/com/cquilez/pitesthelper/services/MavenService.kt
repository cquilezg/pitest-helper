package com.cquilez.pitesthelper.services

import com.cquilez.pitesthelper.model.MutationCoverageCommandData
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.idea.maven.execution.MavenRunner
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.execution.MavenRunnerSettings

object MavenService {
    fun buildPitestArgs(mutationCoverageCommandData: MutationCoverageCommandData) =
        "-DtargetClasses=${mutationCoverageCommandData.targetClasses} -DtargetTests=${mutationCoverageCommandData.targetTests}"

    fun runMavenCommand(project: Project, module: Module, goals: List<String>, vmOptions: String) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow: ToolWindow? = toolWindowManager.getToolWindow("Maven")

        if (toolWindow != null) {
            if (toolWindow.contentManager.contents.isNotEmpty()) {
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