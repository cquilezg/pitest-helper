package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.ProjectElement
import com.cquilez.pitesthelper.model.MutationCoverageData

interface FileDataExtractorFromUiOutPort<T, R> {
    fun extractAnalysisRequestFromUI(inputData: T): R

    fun extractProjectElementsFromRequestData(projectContext: R): List<ProjectElement>

    fun extractSelectedProjectFiles(inputData: T): List<ProjectElement>

    fun processObjects(projectElements: List<ProjectElement>): MutationCoverageData
}