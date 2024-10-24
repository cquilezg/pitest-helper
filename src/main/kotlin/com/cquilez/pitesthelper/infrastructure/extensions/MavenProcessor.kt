package com.cquilez.pitesthelper.infrastructure.extensions

import com.cquilez.pitesthelper.application.port.out.UserNotificationOutPort
import com.cquilez.pitesthelper.domain.ProjectElement
import com.cquilez.pitesthelper.exception.PitestHelperException
import com.cquilez.pitesthelper.infrastructure.buildsystem.AbstractBuildSystemProcessor
import com.cquilez.pitesthelper.infrastructure.dto.ProjectAnalysisRequestDTO
import com.cquilez.pitesthelper.infrastructure.service.ProjectElementService
import com.cquilez.pitesthelper.model.MutationCoverageCommandData
import com.cquilez.pitesthelper.model.MutationCoverageData
import com.cquilez.pitesthelper.services.ExtensionsService
import com.cquilez.pitesthelper.services.MavenService
import com.cquilez.pitesthelper.services.ServiceProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiDirectory

class MavenProcessor : AbstractBuildSystemProcessor() {

    override fun processFiles(
        projectAnalysisRequest: ProjectAnalysisRequestDTO,
        projectFiles: List<ProjectElement>,
        notificationPort: UserNotificationOutPort
    ): MutationCoverageData {
        notificationPort.notifyUser("Maven", "You are using Maven!")

        val project = projectAnalysisRequest.project
        val serviceProvider = project.service<ServiceProvider>()
        val projectElementService = serviceProvider.getService<ProjectElementService>(project)
        val extensionsService = serviceProvider.getService<ExtensionsService>(project)



        val mainSourceModules =
            resolveMainSourceModules(projectElementService, projectAnalysisRequest, extensionsService)
        val testSourceModules = mutableListOf<Module>()

        // Resolve modules -> Crear una implementación de BuildSystemProcessor para los métodos comunes???
        resolveModules()

//        if (navigatable is PsiDirectoryNode) {
//            processDirectory(javaDirectoryService, navigatable, module, mutationCoverage)
//        } else if (projectService.isJavaNavigatable(navigatable)) {
//            processJavaClass(navigatable as ClassTreeNode, module, mutationCoverage)
//        } else if (projectService.isKotlinNavigatable(navigatable)) {
//            processKotlinClass(
//                navigatable,
//                extensionsService.getKotlinExtension().findVirtualFile(navigatable),
//                module,
//                mutationCoverage
//            )
//        }
        return MutationCoverageData(mainSourceModules[0], listOf(), listOf())
    }

    override fun buildCommand(mutationCoverageCommandData: MutationCoverageCommandData) =
        "mvn test-compile pitest:mutationCoverage ${MavenService.buildPitestArgs(mutationCoverageCommandData)}"

    override fun runCommand(projectAnalysisRequest: ProjectAnalysisRequestDTO, mutationCoverageCommandData: MutationCoverageCommandData) {
        MavenService.runMavenCommand(
            projectAnalysisRequest.project,
            mutationCoverageCommandData.module,
            listOf("test-compile", "pitest:mutationCoverage"),
            MavenService.buildPitestArgs(mutationCoverageCommandData)
        )
    }

    private fun resolveMainSourceModules(
        projectElementService: ProjectElementService,
        projectAnalysisRequest: ProjectAnalysisRequestDTO,
        extensionsService: ExtensionsService
    ): List<Module> {
        return projectAnalysisRequest.selectedItems.stream().map {
            var module: Module? = null
            if (it is PsiDirectory) {
                module = ModuleUtilCore.findModuleForFile(it.containingFile.virtualFile, projectAnalysisRequest.project)
            } else if (projectElementService.isJavaPsiFile(it) || projectElementService.isKotlinPsiFile(it)) {
                module = ModuleUtilCore.findModuleForFile(it.containingFile.virtualFile, projectAnalysisRequest.project)
            }
            module
                ?: throw PitestHelperException("There is/are elements not supported. $helpMessage")
        }.distinct().toList()
    }

//    private fun getFileModule(
//        project: Project,
//        extensionsService: ExtensionsService,
//        navigatable: Navigatable
//    ): Module {
//        var module: Module? = null
//        if (isJavaNavigatable(navigatable)) {
//            module = ModuleUtilCore.findModuleForFile(findJavaVirtualFile(navigatable), project)
//        } else if (isKotlinNavigatable(navigatable)) {
//            val kotlinProcessor = extensionsService.getKotlinExtension()
//            module = kotlinProcessor.findNavigatableModule(navigatable)
//        }
//        return module
//            ?: throw PitestHelperException("There is/are elements not supported. $helpMessage")
//    }

    override fun resolveModules() {
//        val moduleList = if (!navigatableArray.isNullOrEmpty()) {
//            listOf(projectService.findNavigatableModule(project, extensionsService, navigatableArray[0]))
//        } else {
//            throw PitestHelperException("Cannot resolve modules because Maven project info is unavailable")
//        }
//        mainSourceModules.addAll(moduleList)
//        testSourceModules.addAll(moduleList)
    }
}