package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.MutationCoverageProjectSettings

interface ProjectConfigPort {
    fun getDefaultSettings(): MutationCoverageProjectSettings
    fun saveSettings(settings: MutationCoverageProjectSettings)
}