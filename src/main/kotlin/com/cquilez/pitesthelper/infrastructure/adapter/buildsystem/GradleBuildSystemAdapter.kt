package com.cquilez.pitesthelper.infrastructure.adapter.buildsystem

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions

class GradleBuildSystemAdapter : AbstractBuildSystemAdapter() {
    override fun getBuildSystem(): BuildSystem = BuildSystem.GRADLE

    override fun buildCommand(mutationCoverageOptions: MutationCoverageOptions): String {
        val pitestGoal = resolvePitestGoal(mutationCoverageOptions)
        val pitestArgs = "-Ppitest.targetClasses=${mutationCoverageOptions.targetClasses} -Ppitest.targetTests=${mutationCoverageOptions.targetTests}"
        return "gradle $pitestGoal $pitestArgs"
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
}

