package com.cquilez.pitesthelper.infrastructure.service.buildsystem

import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.infrastructure.adapter.buildsystem.MavenCommandRunnerAdapter
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class MavenCommandRunnerService(project: Project) {

    private val mavenRunnerAdapter = project.service<MavenCommandRunnerAdapter>()

    fun buildPitestArgs(options: MutationCoverageOptions) =
        "-DtargetClasses=${options.targetClasses} -DtargetTests=${options.targetTests}"

    fun runMutationCoverage(options: MutationCoverageOptions) {
        val buildUnit = options.workingUnit
        val goals = listOf("test-compile", "pitest:mutationCoverage")
        val vmOptions = buildPitestArgs(options)
        mavenRunnerAdapter.runMavenCommand(goals, vmOptions, buildUnit.buildPath.parent)
    }
}