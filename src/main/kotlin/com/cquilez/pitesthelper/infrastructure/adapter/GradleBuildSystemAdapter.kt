package com.cquilez.pitesthelper.infrastructure.adapter

import com.cquilez.pitesthelper.application.port.out.BuildSystemPort
import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions

class GradleBuildSystemAdapter : BuildSystemPort {
    override fun getBuildSystem(): BuildSystem = BuildSystem.GRADLE

    override fun buildCommand(mutationCoverageOptions: MutationCoverageOptions): String {
        val pitestGoal = resolvePitestGoal(mutationCoverageOptions)
        val pitestArgs = "-Ppitest.targetClasses=${mutationCoverageOptions.targetClasses} -Ppitest.targetTests=${mutationCoverageOptions.targetTests}"
        return "gradle $pitestGoal $pitestArgs"
    }

    private fun resolvePitestGoal(options: MutationCoverageOptions): String {
        val workingUnit = options.workingUnit ?: return "pitest"

        if (options.isSubmodule) {
            val moduleName = workingUnit.buildPath.fileName?.toString() ?: return "pitest"
            return ":${moduleName}:pitest"
        }

        return "pitest"
    }
}

