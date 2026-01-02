package com.cquilez.pitesthelper.infrastructure.processor

import com.cquilez.pitesthelper.domain.exception.PitestHelperException
import com.cquilez.pitesthelper.domain.model.MutationCoverageCommand
import com.cquilez.pitesthelper.domain.model.MutationCoverageData
import com.cquilez.pitesthelper.processors.MutationCoverageCommandProcessor
import com.cquilez.pitesthelper.services.*
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiFile

open class MavenMutationCoverageCommandProcessor(
    project: Project,
    projectService: MyProjectService,
    classService: ClassService,
    languageProcessorService: LanguageProcessorService,
    navigatableArray: Array<Navigatable>?,
    psiFile: PsiFile?
) : MutationCoverageCommandProcessor(
    project,
    projectService,
    classService,
    languageProcessorService,
    navigatableArray,
    psiFile
) {

    override fun resolveModules() {
        val moduleList = if (!navigatableArray.isNullOrEmpty()) {
            listOf(projectService.findNavigatableModule(project, languageProcessorService, navigatableArray[0]))
        } else if (psiFile != null) {
            listOf(projectService.getModuleFromElement(psiFile))
        } else {
            throw PitestHelperException("Cannot resolve modules because Maven project info is unavailable")
        }
        mainSourceModules.addAll(moduleList)
        testSourceModules.addAll(moduleList)
    }

    override fun processProjectNodes(navigatableArray: Array<Navigatable>): MutationCoverageData {
        if (navigatableArray.size > 1) {
            checkAllElementsAreInSameModule()
        }
        return super.processProjectNodes(navigatableArray)
    }

    override fun checkAllElementsAreInSameModule() {
        if (navigatableArray != null) {
            if (navigatableArray.isEmpty()) {
                throw PitestHelperException("There are no elements to process")
            }
            var module: Module? = null
            navigatableArray.forEach {
                val newModule: Module = projectService.findNavigatableModule(project, languageProcessorService, it)
                if (module == null) {
                    module = newModule
                } else if (module != newModule) {
                    throw PitestHelperException("You cannot choose elements from different modules")
                }
            }
        }
    }

    override fun buildCommand(mutationCoverageCommandData: MutationCoverageCommand) =
        let {
            val goals = buildGoals(mutationCoverageCommandData).joinToString(" ")
            "mvn $goals ${
                MavenService.buildPitestArgs(
                    mutationCoverageCommandData
                )
            }"
        }

    override fun runCommand(mutationCoverageCommandData: MutationCoverageCommand) {
        MavenService.runMavenCommand(
            project,
            mutationCoverageCommandData.module,
            buildGoals(mutationCoverageCommandData),
            MavenService.buildPitestArgs(mutationCoverageCommandData)
        )
    }

    override fun saveSettings(mutationCoverageCommandData: MutationCoverageCommand) {
        val serviceProvider = project.service<ServiceProvider>()
    }

    private fun buildGoals(mutationCoverageCommandData: MutationCoverageCommand) =
        buildActions(
            mutationCoverageCommandData.preActions, "pitest:mutationCoverage",
            mutationCoverageCommandData.postActions
        )
}
