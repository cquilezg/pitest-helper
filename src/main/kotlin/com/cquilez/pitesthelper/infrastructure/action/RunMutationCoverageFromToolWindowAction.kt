package com.cquilez.pitesthelper.infrastructure.action

import com.cquilez.pitesthelper.application.cases.RunMutationCoverageFromProjectViewUseCase
import com.cquilez.pitesthelper.infrastructure.dto.ProjectAnalysisRequestDTO
import com.cquilez.pitesthelper.infrastructure.service.ActionVisibilityService
import com.cquilez.pitesthelper.infrastructure.service.FileDataExtractorFromUIService
import com.cquilez.pitesthelper.infrastructure.service.UserNotificationService
import com.cquilez.pitesthelper.services.ExtensionsService
import com.cquilez.pitesthelper.services.ServiceProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.annotations.NotNull

/**
 * Run Mutation Coverage action
 */
class RunMutationCoverageFromToolWindowAction : DumbAwareAction() {

    private var runMutationUseCase: RunMutationCoverageFromProjectViewUseCase<AnActionEvent, ProjectAnalysisRequestDTO> = RunMutationCoverageFromProjectViewUseCase()

    /**
     * Update action in menu.
     *
     * @param event Action event.
     */
    override fun update(@NotNull event: AnActionEvent) {
        val project = event.project!!
        event.presentation.isEnabledAndVisible =
            runMutationUseCase.isActionVisible(event, project.service<ServiceProvider>().getService<ActionVisibilityService>(project))
    }

    /**
     * Read event data, create command processor and runs it
     */
    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val project = event.project!!
        val serviceProvider = event.project!!.service<ServiceProvider>()
        val fileDataExtractorFromUI = serviceProvider.getService<FileDataExtractorFromUIService>(project)
        val notificationService = serviceProvider.getService<UserNotificationService>(project)
        val extensionsService = serviceProvider.getService<ExtensionsService>(project)

        runMutationUseCase.runAction(event, fileDataExtractorFromUI, notificationService, extensionsService)
    }

    /**
     * The update() method is called on a background thread
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}