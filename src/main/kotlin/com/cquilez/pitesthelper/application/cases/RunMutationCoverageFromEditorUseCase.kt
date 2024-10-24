package com.cquilez.pitesthelper.application.cases

import com.cquilez.pitesthelper.application.port.`in`.ActionVisibilityInPort
import com.cquilez.pitesthelper.application.port.out.FileDataExtractorFromUiOutPort

class RunMutationCoverageFromEditorUseCase<T, R> {
    fun isActionVisible(data: T, visibilityService: ActionVisibilityInPort<T>): Boolean {
        return visibilityService.isProjectViewActionVisible(data)
    }

    fun runAction(data: T, fileDataExtractor: FileDataExtractorFromUiOutPort<T, R>) {
        // Extract FileProject data
        val projectFiles = fileDataExtractor.extractSelectedProjectFiles(data)


        // Build unit

        // Show dialog
    }
}