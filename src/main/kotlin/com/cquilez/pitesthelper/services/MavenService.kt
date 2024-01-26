package com.cquilez.pitesthelper.services

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.idea.maven.execution.MavenRunner
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.execution.MavenRunnerSettings

object MavenService {
    fun buildPitestArgs(targetClasses: String, targetTests: String) =
        "-DtargetClasses=$targetClasses -DtargetTests=$targetTests"

    fun runMavenCommand(project: Project, module: Module, goals: List<String>, vmOptions: String) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow: ToolWindow? = toolWindowManager.getToolWindow("Maven")

        if (toolWindow != null) {
            if (!toolWindow.isVisible) {
                toolWindow.setAvailable(true, null)
                toolWindow.setType(toolWindow.type, null)
                toolWindow.activate(null)
            }
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

    fun locateSrcMainJava(module: Module): VirtualFile? {
        val moduleRootManager = ModuleRootManager.getInstance(module)
        val sourceRoots = moduleRootManager.sourceRoots
        for (sourceRoot in sourceRoots) {
            if (sourceRoot.path.endsWith("src/main/java", true)) {
                return sourceRoot
            }
        }
        return null
    }
}