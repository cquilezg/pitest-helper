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
        val preTokens = preGoals.split(" ").filter { it.isNotEmpty() }
        val postTokens = postGoals.split(" ").filter { it.isNotEmpty() }
        val preMavenProps = preTokens.filter { it.startsWith("-D") }
        val preGoalsOnly = preTokens.filter { !it.startsWith("-D") }
        val postMavenProps = postTokens.filter { it.startsWith("-D") }
        val postGoalsOnly = postTokens.filter { !it.startsWith("-D") }
        val goals = buildList {
            addAll(preGoalsOnly)
            add("pitest:mutationCoverage")
            addAll(postGoalsOnly)
        }
        val targetClasses = options.targetClasses.trim()
        val targetTests = options.targetTests.trim()
        val mavenProperties = buildList {
            addAll(preMavenProps)
            if (targetClasses.isNotEmpty()) add("-DtargetClasses=$targetClasses")
            if (targetTests.isNotEmpty()) add("-DtargetTests=$targetTests")
            addAll(postMavenProps)
        }
        AbstractBuildSystemAdapter.forBuildSystem(BuildSystem.MAVEN)?.executeCommand(
            project, options.workingUnit, goals, mavenProperties
        )
    }
}
