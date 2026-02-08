package com.cquilez.pitesthelper.infrastructure.service.buildsystem

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.infrastructure.adapter.buildsystem.AbstractBuildSystemAdapter
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class MavenBuildSystemService(private val project: Project) : BuildSystemService {

    override fun buildCommand(mutationCoverageOptions: MutationCoverageOptions): String {
        val preGoals = normalizeAndResolveActions(mutationCoverageOptions.preActions.trim(), mutationCoverageOptions)
        val postGoals = normalizeAndResolveActions(mutationCoverageOptions.postActions.trim(), mutationCoverageOptions)
        val targetClasses = mutationCoverageOptions.targetClasses.trim()
        val targetTests = mutationCoverageOptions.targetTests.trim()
        val pitestGoal = "pitest:mutationCoverage"

        val goals = buildList {
            if (preGoals.isNotEmpty()) add(preGoals)
            add(pitestGoal)
            if (postGoals.isNotEmpty()) add(postGoals)
        }.joinToString(" ")

        val args = buildList {
            if (targetClasses.isNotEmpty()) add("-DtargetClasses=$targetClasses")
            if (targetTests.isNotEmpty()) add("-DtargetTests=$targetTests")
        }.joinToString(" ")

        return if (args.isNotEmpty()) "mvn $goals $args" else "mvn $goals"
    }

    fun runMutationCoverage(options: MutationCoverageOptions) {
        val preGoals = normalizeAndResolveActions(options.preActions.trim(), options)
        val postGoals = normalizeAndResolveActions(options.postActions.trim(), options)
        val goals = buildList {
            preGoals.split(" ").filter { it.isNotEmpty() }.forEach { add(it) }
            add("pitest:mutationCoverage")
            postGoals.split(" ").filter { it.isNotEmpty() }.forEach { add(it) }
        }
        val targetClasses = options.targetClasses.trim()
        val targetTests = options.targetTests.trim()
        val mavenPropertiesArg = listOfNotNull(
            if (targetClasses.isNotEmpty()) "-DtargetClasses=$targetClasses" else null,
            if (targetTests.isNotEmpty()) "-DtargetTests=$targetTests" else null
        ).joinToString(" ")
        AbstractBuildSystemAdapter.forBuildSystem(BuildSystem.MAVEN)?.executeCommand(
            project, options.workingUnit, goals, mavenPropertiesArg
        )
    }
}
