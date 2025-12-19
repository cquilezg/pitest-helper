package com.cquilez.pitesthelper.domain

import com.intellij.openapi.module.Module

data class MutationCoverageOptions(
    var module: Module?,
    var preActions: String = "",
    var postActions: String = "",
    var targetClasses: String = "",
    var targetTests: String = "",
    var errors: List<String> = emptyList(),
    var workingUnit: BuildUnit? = null
) {
    fun buildCommand(buildSystem: BuildSystem): String {
        return when (buildSystem) {
            BuildSystem.MAVEN -> buildMavenCommand()
            BuildSystem.GRADLE -> buildGradleCommand()
            else -> {
                buildMavenCommand()
            }
        }
    }

    private fun buildMavenCommand(): String {
        val goals = buildGoalsList("pitest:mutationCoverage")
        val pitestArgs = buildMavenPitestArgs()
        return "mvn ${goals.joinToString(" ")} $pitestArgs"
    }

    private fun buildGradleCommand(): String {
        val tasks = buildGoalsList("pitest")
        val pitestArgs = buildGradlePitestArgs()
        return "gradle ${tasks.joinToString(" ")} $pitestArgs"
    }

    private fun buildGoalsList(pitestGoal: String): List<String> {
        return (preActions.split(" ") + pitestGoal + postActions.split(" "))
            .filter { it.isNotBlank() }
    }

    private fun buildMavenPitestArgs(): String {
        val args = mutableListOf<String>()
        if (targetClasses.isNotBlank()) {
            args.add("-DtargetClasses=$targetClasses")
        }
        if (targetTests.isNotBlank()) {
            args.add("-DtargetTests=$targetTests")
        }
        return args.joinToString(" ")
    }

    private fun buildGradlePitestArgs(): String {
        val args = mutableListOf<String>()
        if (targetClasses.isNotBlank()) {
            args.add("-Ppitest.targetClasses=$targetClasses")
        }
        if (targetTests.isNotBlank()) {
            args.add("-Ppitest.targetTests=$targetTests")
        }
        return args.joinToString(" ")
    }
}