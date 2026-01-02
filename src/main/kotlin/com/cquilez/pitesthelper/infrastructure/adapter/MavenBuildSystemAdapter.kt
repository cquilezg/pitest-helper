package com.cquilez.pitesthelper.infrastructure.adapter

import com.cquilez.pitesthelper.application.port.out.BuildSystemPort
import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions

class MavenBuildSystemAdapter : BuildSystemPort {
    override fun getBuildSystem(): BuildSystem = BuildSystem.MAVEN

    override fun buildCommand(mutationCoverageOptions: MutationCoverageOptions): String {
        val pitestGoal = "test-compile pitest:mutationCoverage"
        val pitestArgs = "-DtargetClasses=${mutationCoverageOptions.targetClasses} -DtargetTests=${mutationCoverageOptions.targetTests}"
        return "mvn $pitestGoal $pitestArgs"
    }
}

