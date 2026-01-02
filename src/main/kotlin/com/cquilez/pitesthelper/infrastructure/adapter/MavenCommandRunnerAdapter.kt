package com.cquilez.pitesthelper.infrastructure.adapter

import com.cquilez.pitesthelper.services.MavenService.POM_FILE
import com.cquilez.pitesthelper.services.MavenService.getCombinedJvmConfig
import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.idea.maven.execution.MavenRunner
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.execution.MavenRunnerSettings
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class MavenCommandRunnerAdapter(private val project: Project) {

    fun runMavenCommand(goals: List<String>, pitestArgs: String, workingDirectory: Path) {
        val module = ModuleManager.getInstance(project).modules.find { module ->
            module.rootManager.contentRoots.any { it.path == workingDirectory.toString() }
        } ?: return

        runMavenCommand(project, module, goals, pitestArgs)
    }

    private fun runMavenCommand(project: Project, module: Module, goals: List<String>, vmOptions: String) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow: ToolWindow? = toolWindowManager.getToolWindow("Maven")

        if (toolWindow != null && toolWindow.contentManager.contents.isNotEmpty()
            && module.rootManager.contentRoots.isNotEmpty()
        ) {
            val contentRoot = module.rootManager.contentRoots.firstOrNull()
            val pomFile = contentRoot?.findChild(POM_FILE)
            val parameters = MavenRunnerParameters(
                true, module.rootManager.contentRoots[0].path, POM_FILE, goals, emptyList()
            )
            val mavenRunner = MavenRunner(project)
            val mavenRunnerSettings = MavenRunnerSettings()
            mavenRunnerSettings.setVmOptions(getCombinedJvmConfig(project, pomFile) + " " + vmOptions)
            mavenRunner.run(parameters, mavenRunnerSettings) { }
        }
    }
}