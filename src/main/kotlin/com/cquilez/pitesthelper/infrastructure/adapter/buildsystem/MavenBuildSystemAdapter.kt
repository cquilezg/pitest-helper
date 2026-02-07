package com.cquilez.pitesthelper.infrastructure.adapter.buildsystem

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions

class MavenBuildSystemAdapter : AbstractBuildSystemAdapter() {
    override fun getBuildSystem(): BuildSystem = BuildSystem.MAVEN

    override fun buildCommand(mutationCoverageOptions: MutationCoverageOptions): String {
        val preGoals = normalizeAndResolveGoals(mutationCoverageOptions.preActions.trim(), mutationCoverageOptions)
        val postGoals = normalizeAndResolveGoals(mutationCoverageOptions.postActions.trim(), mutationCoverageOptions)
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

    private fun normalizeAndResolveGoals(goals: String, options: MutationCoverageOptions): String {
        if (goals.isEmpty()) return ""

        // Normalize whitespace: replace multiple spaces with single space
        val normalized = goals.replace(Regex("\\s+"), " ").trim()

        // If it's a submodule, prefix each goal with :moduleName:
        val workingUnit = options.workingUnit
        if (options.buildUnits.isNotEmpty()) {
            val firstBuildUnit = options.buildUnits[0]
            if (workingUnit != firstBuildUnit && firstBuildUnit.buildUnits.size > 1) {
                val moduleName = workingUnit.name
                return normalized.split(" ").joinToString(" ") { goal ->
                    if (goal.startsWith(":")) goal else ":$moduleName:$goal"
                }
            }
        }

        return normalized
    }
}

