package com.cquilez.pitesthelper.infrastructure.adapter.buildsystem

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions

class GradleBuildSystemAdapter : AbstractBuildSystemAdapter() {
    override fun getBuildSystem(): BuildSystem = BuildSystem.GRADLE

    override fun buildCommand(mutationCoverageOptions: MutationCoverageOptions): String {
        val preTasks = normalizeAndResolveTasks(mutationCoverageOptions.preActions.trim(), mutationCoverageOptions)
        val postTasks = normalizeAndResolveTasks(mutationCoverageOptions.postActions.trim(), mutationCoverageOptions)
        val targetClasses = mutationCoverageOptions.targetClasses.trim()
        val targetTests = mutationCoverageOptions.targetTests.trim()
        val pitestTask = resolvePitestGoal(mutationCoverageOptions)

        val tasks = buildList {
            if (preTasks.isNotEmpty()) add(preTasks)
            add(pitestTask)
            if (postTasks.isNotEmpty()) add(postTasks)
        }.joinToString(" ")

        val args = buildList {
            if (targetClasses.isNotEmpty()) add("-Ppitest.targetClasses=$targetClasses")
            if (targetTests.isNotEmpty()) add("-Ppitest.targetTests=$targetTests")
        }.joinToString(" ")

        return if (args.isNotEmpty()) "gradle $tasks $args" else "gradle $tasks"
    }

    private fun resolvePitestGoal(options: MutationCoverageOptions): String {
        val workingUnit = options.workingUnit

        val firstBuildUnit = options.buildUnits[0]
        if (workingUnit != firstBuildUnit && firstBuildUnit.buildUnits.size > 1) {
            val moduleName = workingUnit.name
            return ":${moduleName}:pitest"
        }

        return "pitest"
    }

    private fun normalizeAndResolveTasks(tasks: String, options: MutationCoverageOptions): String {
        if (tasks.isEmpty()) return ""
        
        // Normalize whitespace: replace multiple spaces with single space
        val normalized = tasks.replace(Regex("\\s+"), " ").trim()
        
        // If it's a submodule, prefix each task with :moduleName: (using same logic as resolvePitestGoal)
        val workingUnit = options.workingUnit
        if (options.buildUnits.isNotEmpty()) {
            val firstBuildUnit = options.buildUnits[0]
            if (workingUnit != firstBuildUnit && firstBuildUnit.buildUnits.size > 1) {
                val moduleName = workingUnit.name
                return normalized.split(" ").joinToString(" ") { task ->
                    if (task.startsWith(":")) task else ":$moduleName:$task"
                }
            }
        }
        
        return normalized
    }
}

