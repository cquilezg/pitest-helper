package com.cquilez.pitesthelper.infrastructure.service.buildsystem

import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.infrastructure.adapter.buildsystem.GradleCommandRunnerAdapter
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class GradleCommandRunnerService(project: Project) {

    private val gradleRunnerAdapter = project.service<GradleCommandRunnerAdapter>()

    fun buildPitestArgs(options: MutationCoverageOptions) =
        "-Ppitest.targetClasses=${options.targetClasses} -Ppitest.targetTests=${options.targetTests}"

    fun runMutationCoverage(options: MutationCoverageOptions) {
        val pitestGoal = buildPitestGoal(options)
        val pitestArgs = buildPitestArgs(options)
        gradleRunnerAdapter.runGradleCommand("$pitestGoal $pitestArgs")
    }

    private fun buildPitestGoal(options: MutationCoverageOptions): String {
        if (options.isSubmodule) {
            val moduleName = extractModuleName(options.workingUnit.buildPath.parent?.fileName?.toString() ?: "")
            if (moduleName.isNotBlank()) {
                return ":${moduleName}:pitest"
            }
        }
        return "pitest"
    }

    private fun extractModuleName(path: String): String {
        return path.ifBlank { "" }
    }
}
