package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.MutationCoverageProjectSettings

fun interface ProjectConfigPort {
    fun getDefaultSettings(): MutationCoverageProjectSettings
}