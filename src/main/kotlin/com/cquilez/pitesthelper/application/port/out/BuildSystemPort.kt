package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions

interface BuildSystemPort {
    fun getBuildSystem(): BuildSystem
    fun buildCommand(mutationCoverageOptions: MutationCoverageOptions): String
}