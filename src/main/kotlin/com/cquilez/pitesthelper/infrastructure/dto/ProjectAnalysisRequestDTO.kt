package com.cquilez.pitesthelper.infrastructure.dto

import com.intellij.openapi.project.Project

data class ProjectAnalysisRequestDTO(val project: Project, val selectedItems: List<ProjectElementDataDTO>)
