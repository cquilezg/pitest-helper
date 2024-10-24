package com.cquilez.pitesthelper.infrastructure.extensions

import com.cquilez.pitesthelper.application.port.out.UserNotificationOutPort
import com.cquilez.pitesthelper.domain.ProjectElement
import com.cquilez.pitesthelper.exception.PitestHelperException
import com.cquilez.pitesthelper.infrastructure.buildsystem.AbstractBuildSystemProcessor
import com.cquilez.pitesthelper.infrastructure.dto.ProjectAnalysisRequestDTO
import com.cquilez.pitesthelper.infrastructure.dto.SourceModules
import com.cquilez.pitesthelper.infrastructure.service.ProjectElementService
import com.cquilez.pitesthelper.model.MutationCoverageCommandData
import com.cquilez.pitesthelper.model.MutationCoverageData
import com.cquilez.pitesthelper.services.ExtensionsService
import com.cquilez.pitesthelper.services.ServiceProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiDirectory

class GradleProcessor : AbstractBuildSystemProcessor() {
    override fun resolveModules(
        projectElementService: ProjectElementService,
        projectAnalysisRequest: ProjectAnalysisRequestDTO
    ): SourceModules {
        projectAnalysisRequest.selectedItems.stream().map {
            if (it is PsiDirectory) {
                ModuleUtilCore.findModuleForFile(it.virtualFile, projectAnalysisRequest.project)
            } else if (projectElementService.isJavaPsiFile(it)) {
                ModuleUtilCore.findModuleForFile(it.containingFile.virtualFile, projectAnalysisRequest.project)
            } else {
                throw PitestHelperException("There is/are elements not supported. $helpMessage")
            }
        }.toList()
        val mainModules =
    }

    override fun processFiles(
        projectAnalysisRequest: ProjectAnalysisRequestDTO,
        projectFiles: List<ProjectElement>,
        notificationPort: UserNotificationOutPort
    ): MutationCoverageData {
        notificationPort.notifyUser("Gradle", "You are using Gradle!")

        val project = projectAnalysisRequest.project
        val serviceProvider = project.service<ServiceProvider>()
        val projectElementService = serviceProvider.getService<ProjectElementService>(project)
        val extensionsService = serviceProvider.getService<ExtensionsService>(project)

        val mainSourceModules =
            resolveMainSourceModules(projectElementService, projectAnalysisRequest, extensionsService)
        val testSourceModules = mutableListOf<Module>()

        // Resolve modules -> Crear una implementación de BuildSystemProcessor para los métodos comunes???
        resolveModules()

        return MutationCoverageData(mainSourceModules[0], listOf(), listOf())
    }

    override fun buildCommand(mutationCoverageCommandData: MutationCoverageCommandData): String {
        TODO("Not yet implemented")
    }

    override fun runCommand(
        projectAnalysisRequest: ProjectAnalysisRequestDTO,
        mutationCoverageCommandData: MutationCoverageCommandData
    ) {
        TODO("Not yet implemented")
    }

    private fun resolveMainSourceModules(
        projectElementService: ProjectElementService,
        projectAnalysisRequest: ProjectAnalysisRequestDTO,
        extensionsService: ExtensionsService
    ): List<Module> {
        return projectAnalysisRequest.selectedItems.stream().map {
            var module: Module? = null
            if (it is PsiDirectory) {
                ModuleUtilCore.findModuleForFile(it.virtualFile, projectAnalysisRequest.project)
            } else if (projectElementService.isJavaPsiFile(it)) {
                module = ModuleUtilCore.findModuleForFile(it.containingFile.virtualFile, projectAnalysisRequest.project)
            }
            module
                ?: throw PitestHelperException("There is/are elements not supported. $helpMessage")
        }.toList()
    }

}