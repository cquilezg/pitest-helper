package com.cquilez.pitesthelper.application.cases

import com.cquilez.pitesthelper.application.port.`in`.ActionVisibilityInPort
import com.cquilez.pitesthelper.application.port.BuildSystemProcessor
import com.cquilez.pitesthelper.application.port.out.ExtensionsOutPort
import com.cquilez.pitesthelper.application.port.out.FileDataExtractorFromUiOutPort
import com.cquilez.pitesthelper.application.port.out.UserNotificationOutPort
import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.ProjectElement
import com.cquilez.pitesthelper.exception.PitestHelperException

class RunMutationCoverageFromProjectViewUseCase<T, U> {
    fun isActionVisible(data: T, visibilityService: ActionVisibilityInPort<T>): Boolean {
        return visibilityService.isProjectViewActionVisible(data)
    }

    fun runAction(
        data: T,
        fileDataExtractor: FileDataExtractorFromUiOutPort<T, U>,
        notificationPort: UserNotificationOutPort,
        extensionsService: ExtensionsOutPort
    ) {
        // TODO: as we have to imitate Code Coverage behavior, check if we need to get module and source root or
        //  we have to throw an exception
        // TODO: extracting NavigatablePsiClass, virtual file, module and source root
        val projectAnalysisRequest = fileDataExtractor.extractAnalysisRequestFromUI(data)

        // Extract paths from NavigatablePsiElement's
        val projectElements = fileDataExtractor.extractProjectElementsFromRequestData(projectAnalysisRequest)

        checkAllElementsAreInSameBuildUnit(projectElements, notificationPort)

        val buildSystemExtension = getBuildSystem(extensionsService, projectElements[0].buildUnit.buildSystem)
        val mutationCoverageData =
            buildSystemExtension.processFiles(projectAnalysisRequest, projectElements, notificationPort)

        // Show Run Mutation Coverage dialog

    }

    private fun checkAllElementsAreInSameBuildUnit(
        projectElements: List<ProjectElement>,
        notificationPort: UserNotificationOutPort
    ) {
        val firstBuildUnit = projectElements[0].buildUnit
        val sameBuildUnit = projectElements.all { it.buildUnit == firstBuildUnit }
        notificationPort.notifyUser(
            "Project Elements",
            projectElements.joinToString { "Path: ${it.path}, BuildUnit: (${it.buildUnit.buildPath}, ${it.buildUnit.buildSystem}, ${it.buildUnit.buildFileName})" })
        if (!sameBuildUnit) {
            notificationPort.notifyUser("Run Mutation Coverage", "The elements are not in the same build unit!")
            throw PitestHelperException("The elements are not in the same build unit!")
        }
    }

    private fun getBuildSystem(extensionsPort: ExtensionsOutPort, buildSystem: BuildSystem): BuildSystemProcessor<U> {
        return if (buildSystem == BuildSystem.MAVEN) {
            extensionsPort.getMavenExtension()
        } else {
            extensionsPort.getGradleExtension()
        }
    }
}