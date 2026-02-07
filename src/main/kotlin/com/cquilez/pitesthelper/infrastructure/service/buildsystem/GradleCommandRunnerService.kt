package com.cquilez.pitesthelper.infrastructure.service.buildsystem

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.infrastructure.adapter.buildsystem.AbstractBuildSystemAdapter
import com.cquilez.pitesthelper.infrastructure.adapter.buildsystem.GradleCommandRunnerAdapter
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class GradleCommandRunnerService(project: Project) {

    private val gradleRunnerAdapter = project.service<GradleCommandRunnerAdapter>()

    fun runMutationCoverage(options: MutationCoverageOptions) {
        val buildSystemPort = AbstractBuildSystemAdapter.forBuildSystem(BuildSystem.GRADLE) ?: return
        val fullCommand = buildSystemPort.buildCommand(options)
        // fullCommand is "gradle task1 task2 ... -Pkey=val ..."
        val commandWithoutPrefix = fullCommand.removePrefix("gradle ").trim()
        gradleRunnerAdapter.runGradleCommand(commandWithoutPrefix)
    }
}
