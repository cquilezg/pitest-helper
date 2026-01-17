package com.cquilez.pitesthelper.infrastructure.adapter.buildsystem

import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.idea.maven.execution.MavenRunner
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.execution.MavenRunnerSettings
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.io.File
import java.nio.file.Path

private const val JVM_CONFIG_FILE = ".mvn/jvm.config"
private const val POM_FILE = "pom.xml"

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

    private fun getCombinedJvmConfig(project: Project, currentPom: VirtualFile?): String {
        val mavenManager = MavenProjectsManager.getInstance(project)
        if (currentPom == null) {
            return ""
        }

        val currentMavenProject = mavenManager.findProject(currentPom) ?: return ""

        var jvmConfig = readJvmConfig(File(currentMavenProject.directory, JVM_CONFIG_FILE))
        if (jvmConfig.isNotBlank()) {
            return jvmConfig
        }

        val parentId = currentMavenProject.parentId
        parentId?.let { id ->
            mavenManager.projects.find { it.mavenId == id }?.let { parentProject ->
                jvmConfig = readJvmConfig(File(parentProject.directory, JVM_CONFIG_FILE))
            }
        }

        if (jvmConfig.isNotBlank()) {
            return jvmConfig
        }
        return ""
    }

    fun readJvmConfig(file: File): String {
        return if (file.exists()) {
            file.readLines()
                .filter { it.isNotBlank() && !it.trim().startsWith("#") }
                .joinToString(" ")
        } else ""
    }
}