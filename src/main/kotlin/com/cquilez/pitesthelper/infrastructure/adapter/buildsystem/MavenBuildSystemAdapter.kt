package com.cquilez.pitesthelper.infrastructure.adapter.buildsystem

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.BuildUnit
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

class MavenBuildSystemAdapter : AbstractBuildSystemAdapter() {
    private val jvmConfigFile = ".mvn/jvm.config"
    private val pomFile = "pom.xml"

    override fun getBuildSystem(): BuildSystem = BuildSystem.MAVEN

    override fun executeCommand(
        project: Project,
        buildUnit: BuildUnit,
        goalsOrTasks: List<String>,
        propertiesArg: String
    ) {
        val workingDirectory = buildUnit.buildPath.parent
        val module = ModuleManager.getInstance(project).modules.find { module ->
            module.rootManager.contentRoots.any { it.path == workingDirectory.toString() }
        } ?: return

        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow: ToolWindow? = toolWindowManager.getToolWindow("Maven")

        if (toolWindow != null && toolWindow.contentManager.contents.isNotEmpty()
            && module.rootManager.contentRoots.isNotEmpty()
        ) {
            val contentRoot = module.rootManager.contentRoots.firstOrNull()
            val pomFile = contentRoot?.findChild(pomFile)
            val parameters = MavenRunnerParameters(
                true, module.rootManager.contentRoots[0].path,
                this@MavenBuildSystemAdapter.pomFile, goalsOrTasks, emptyList()
            )
            val mavenRunner = MavenRunner(project)
            val mavenRunnerSettings = MavenRunnerSettings()
            mavenRunnerSettings.mavenProperties = parseMavenProperties(propertiesArg)
            mavenRunnerSettings.setVmOptions(getCombinedJvmConfig(project, pomFile).trim())
            mavenRunner.run(parameters, mavenRunnerSettings) { }
        }
    }

    private fun parseMavenProperties(mavenPropertiesArg: String): Map<String, String> {
        if (mavenPropertiesArg.isBlank()) return emptyMap()
        return mavenPropertiesArg.split(" ")
            .map { it.trim() }
            .filter { it.startsWith("-D") }.associate { token ->
                val keyValue = token.removePrefix("-D")
                val eq = keyValue.indexOf('=')
                if (eq < 0) keyValue to "" else keyValue.substring(0, eq) to keyValue.substring(eq + 1)
            }
    }

    private fun getCombinedJvmConfig(project: Project, currentPom: VirtualFile?): String {
        val mavenManager = MavenProjectsManager.getInstance(project)
        if (currentPom == null) {
            return ""
        }

        val currentMavenProject = mavenManager.findProject(currentPom) ?: return ""

        var jvmConfig = readJvmConfig(File(currentMavenProject.directory, jvmConfigFile))
        if (jvmConfig.isNotBlank()) {
            return jvmConfig
        }

        val parentId = currentMavenProject.parentId
        parentId?.let { id ->
            mavenManager.projects.find { it.mavenId == id }?.let { parentProject ->
                jvmConfig = readJvmConfig(File(parentProject.directory, jvmConfigFile))
            }
        }

        if (jvmConfig.isNotBlank()) {
            return jvmConfig
        }
        return ""
    }

    private fun readJvmConfig(file: File): String {
        return if (file.exists()) {
            file.readLines()
                .filter { it.isNotBlank() && !it.trim().startsWith("#") }
                .joinToString(" ")
        } else ""
    }
}
