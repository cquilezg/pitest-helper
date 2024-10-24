package com.cquilez.pitesthelper.infrastructure.buildsystem

import com.cquilez.pitesthelper.application.port.BuildSystemProcessor
import com.cquilez.pitesthelper.infrastructure.dto.ProjectAnalysisRequestDTO
import com.cquilez.pitesthelper.infrastructure.dto.ProjectElementDataDTO
import com.cquilez.pitesthelper.infrastructure.dto.SourceModules
import com.cquilez.pitesthelper.infrastructure.service.ProjectElementService

abstract class AbstractBuildSystemProcessor : BuildSystemProcessor<ProjectAnalysisRequestDTO> {

    protected val helpMessage =
        "Please select only Java/Kotlin classes, packages or a module folder containing Java/Kotlin source code."

    protected abstract fun resolveModules(projectElementService: ProjectElementService,
                                          projectAnalysisRequest: ProjectAnalysisRequestDTO,): SourceModules

    protected fun collectProjectElementData(projectAnalysisRequest: ProjectAnalysisRequestDTO): List<ProjectElementDataDTO> {
        projectAnalysisRequest.selectedItems.map {
            if ()
            ProjectElementDataDTO()
        }
    }
}