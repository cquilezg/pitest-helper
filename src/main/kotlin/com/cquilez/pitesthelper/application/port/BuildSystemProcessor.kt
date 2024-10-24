package com.cquilez.pitesthelper.application.port

import com.cquilez.pitesthelper.application.port.out.UserNotificationOutPort
import com.cquilez.pitesthelper.domain.ProjectElement
import com.cquilez.pitesthelper.model.MutationCoverageCommandData
import com.cquilez.pitesthelper.model.MutationCoverageData

interface BuildSystemProcessor<T> {
    fun processFiles(
        projectAnalysisRequest: T,
        projectFiles: List<ProjectElement>,
        notificationPort: UserNotificationOutPort
    ): MutationCoverageData

    fun buildCommand(mutationCoverageCommandData: MutationCoverageCommandData): String
    fun runCommand(projectAnalysisRequest: T, mutationCoverageCommandData: MutationCoverageCommandData)
}