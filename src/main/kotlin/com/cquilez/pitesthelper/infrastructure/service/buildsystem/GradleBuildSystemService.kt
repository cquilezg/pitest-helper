package com.cquilez.pitesthelper.infrastructure.service.buildsystem

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.infrastructure.adapter.buildsystem.AbstractBuildSystemAdapter
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class GradleBuildSystemService(private val project: Project) : BuildSystemService {

    override fun buildCommand(mutationCoverageOptions: MutationCoverageOptions): String {
        val preTasks = normalizeAndResolveActions(mutationCoverageOptions.preActions.trim(), mutationCoverageOptions)
        val postTasks = normalizeAndResolveActions(mutationCoverageOptions.postActions.trim(), mutationCoverageOptions)
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

    fun runMutationCoverage(options: MutationCoverageOptions) {
        val preTasks = normalizeAndResolveActions(options.preActions.trim(), options)
        val postTasks = normalizeAndResolveActions(options.postActions.trim(), options)
        val pitestTask = resolvePitestGoal(options)
        val tasks = buildList {
            preTasks.split(" ").filter { it.isNotEmpty() }.forEach { add(it) }
            add(pitestTask)
            postTasks.split(" ").filter { it.isNotEmpty() }.forEach { add(it) }
        }
        val targetClasses = options.targetClasses.trim()
        val targetTests = options.targetTests.trim()
        val mavenProperties = listOfNotNull(
            if (targetClasses.isNotEmpty()) "-Ppitest.targetClasses=$targetClasses" else null,
            if (targetTests.isNotEmpty()) "-Ppitest.targetTests=$targetTests" else null
        )

        AbstractBuildSystemAdapter.forBuildSystem(BuildSystem.GRADLE)?.executeCommand(
            project, options.workingUnit, tasks, mavenProperties
        )
    }

    private fun resolvePitestGoal(options: MutationCoverageOptions): String {
        if (options.buildUnits.isEmpty()) return "pitest"
        val workingUnit = options.workingUnit
        val firstBuildUnit = options.buildUnits[0]
        if (workingUnit != firstBuildUnit && firstBuildUnit.buildUnits.size > 1) {
            val moduleName = workingUnit.name
            return ":$moduleName:pitest"
        }
        return "pitest"
    }
}
