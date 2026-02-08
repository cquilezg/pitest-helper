package com.cquilez.pitesthelper.infrastructure.action

import com.cquilez.pitesthelper.application.cqrs.command.RunMutationCoverageFromProjectViewCommand
import com.cquilez.pitesthelper.application.port.`in`.RunMutationCoverageFromProjectViewPort
import com.cquilez.pitesthelper.infrastructure.service.actionvisibility.ActionFromProjectViewVisibilityService
import com.cquilez.pitesthelper.infrastructure.ui.NavigatablePort
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NotNull

class RunMutationCoverageFromProjectViewAction : DumbAwareAction() {

    override fun update(@NotNull event: AnActionEvent) {
        val actionVisibilityService =
            ApplicationManager.getApplication().service<ActionFromProjectViewVisibilityService>()
        event.presentation.isEnabledAndVisible = actionVisibilityService.isActionVisible(event)
    }

    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val project = event.project as Project
        val navigatables = event.getData(CommonDataKeys.NAVIGATABLE_ARRAY)
            ?: return
        if (navigatables.isEmpty()) return

        val navigatableService = ApplicationManager.getApplication().service<NavigatablePort>()
        val useCase = project.service<RunMutationCoverageFromProjectViewPort>()
        useCase.execute(
            RunMutationCoverageFromProjectViewCommand(
                navigatableService.getAbsolutePaths(navigatables)
            ),
        )
    }

    /**
     * The update() method is called on a background thread
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}