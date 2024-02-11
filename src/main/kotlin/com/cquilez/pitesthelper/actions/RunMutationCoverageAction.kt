package com.cquilez.pitesthelper.actions

import com.cquilez.pitesthelper.exception.PitestHelperException
import com.cquilez.pitesthelper.model.MutationCoverageData
import com.cquilez.pitesthelper.processors.MutationCoverageCommandProcessor
import com.cquilez.pitesthelper.services.ClassService
import com.cquilez.pitesthelper.services.MyProjectService
import com.cquilez.pitesthelper.services.ServiceProvider
import com.cquilez.pitesthelper.services.UIService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.TestOnly

/**
 * Run Mutation Coverage action
 */
class RunMutationCoverageAction : DumbAwareAction() {

    /**
     * Object only needed for unit tests
     */
    @TestOnly
    companion object {
        var mutationCoverageData: MutationCoverageData? = null
    }

    /**
     * Update action in menu.
     *
     * @param event Action event.
     */
    override fun update(@NotNull event: AnActionEvent) {
        var visible = false
        val project = event.project
        if (project != null) {
            val serviceProvider = project.service<ServiceProvider>()
            val classService = serviceProvider.getService<ClassService>(project)
            val psiFile = event.getData(CommonDataKeys.PSI_FILE)
            // TODO: Check if the directory is inside of a module and in a source root
            if (psiFile != null && classService.isCodeFile(psiFile)) {
                visible = true
            } else {
                val navigatableArray = event.getData(CommonDataKeys.NAVIGATABLE_ARRAY)
                if (!navigatableArray.isNullOrEmpty()) {
                    visible = true
                }
            }
        }

        event.presentation.isEnabledAndVisible = visible
    }

    /**
     * Read event data, create command processor and runs it
     */
    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val project = event.project as Project
        val serviceProvider = project.service<ServiceProvider>()
        val projectService = serviceProvider.getService<MyProjectService>(project)
        val uiService = serviceProvider.getService<UIService>(project)
        val classService = serviceProvider.getService<ClassService>(project)
        val processor = projectService.getBuildSystemProcessor(project)
        processor.processProjectNodes(project)
        uiService.showDialog { Messages.showInfoMessage(project, "Gradle & Maven", "Build system: ${projectService.getBuildSystem(project).name}") }

        val navigatableArray = event.getData(CommonDataKeys.NAVIGATABLE_ARRAY)
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)

        try {
            val mutationCoverageCommandProcessor = MutationCoverageCommandProcessor(project, projectService, classService, uiService)
            mutationCoverageCommandProcessor.handleCommand(navigatableArray, psiFile)
        } catch (e: PitestHelperException) {
            Messages.showErrorDialog(project, e.message, "Unable To Run Mutation Coverage")
        }
    }

    /**
     * The update() method is called on a background thread
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}