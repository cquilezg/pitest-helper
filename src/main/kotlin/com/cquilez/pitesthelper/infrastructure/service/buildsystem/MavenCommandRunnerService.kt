package com.cquilez.pitesthelper.infrastructure.service.buildsystem

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.infrastructure.adapter.buildsystem.AbstractBuildSystemAdapter
import com.cquilez.pitesthelper.infrastructure.adapter.buildsystem.MavenCommandRunnerAdapter
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class MavenCommandRunnerService(project: Project) {

    private val mavenRunnerAdapter = project.service<MavenCommandRunnerAdapter>()

    fun runMutationCoverage(options: MutationCoverageOptions) {
        val buildSystemPort = AbstractBuildSystemAdapter.forBuildSystem(BuildSystem.MAVEN) ?: return
        val fullCommand = buildSystemPort.buildCommand(options)
        // fullCommand is "mvn goal1 goal2 ... -Dkey=val ..."
        val afterMvn = fullCommand.removePrefix("mvn ").trim()
        val tokens = afterMvn.split(" ")
        val goals = mutableListOf<String>()
        val vmOptionsParts = mutableListOf<String>()
        var inArgs = false
        for (token in tokens) {
            if (token.startsWith("-D")) {
                inArgs = true
                vmOptionsParts.add(token)
            } else if (!inArgs) {
                goals.add(token)
            }
        }
        val vmOptions = vmOptionsParts.joinToString(" ")
        val buildUnit = options.workingUnit
        mavenRunnerAdapter.runMavenCommand(goals, vmOptions, buildUnit.buildPath.parent)
    }
}