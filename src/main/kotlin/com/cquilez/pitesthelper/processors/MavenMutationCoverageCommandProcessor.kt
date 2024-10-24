package com.cquilez.pitesthelper.processors

import com.cquilez.pitesthelper.exception.PitestHelperException
import com.cquilez.pitesthelper.services.ExtensionsService
import com.cquilez.pitesthelper.model.MutationCoverageCommandData
import com.cquilez.pitesthelper.model.MutationCoverageData
import com.cquilez.pitesthelper.services.ClassService
import com.cquilez.pitesthelper.services.MavenService
import com.cquilez.pitesthelper.services.MyProjectService
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiFile

open class MavenMutationCoverageCommandProcessor(
    project: Project,
    projectService: MyProjectService,
    classService: ClassService,
    extensionsService: ExtensionsService,
    navigatableArray: Array<Navigatable>?,
    psiFile: PsiFile?
) : MutationCoverageCommandProcessor(
    project,
    projectService,
    classService,
    extensionsService,
    navigatableArray,
    psiFile
) {

    override fun resolveModules() {
        val moduleList = if (!navigatableArray.isNullOrEmpty()) {
            listOf(projectService.findNavigatableModule(project, extensionsService, navigatableArray[0]))
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
                val newModule: Module = projectService.findNavigatableModule(project, extensionsService, it)
                if (module == null) {
                    module = newModule
                } else if (module != newModule) {
                    throw PitestHelperException("You cannot choose elements from different modules")
                }
            }
        }
    }

    override fun buildCommand(mutationCoverageCommandData: MutationCoverageCommandData) =
        "mvn test-compile pitest:mutationCoverage ${MavenService.buildPitestArgs(mutationCoverageCommandData)}"

    override fun runCommand(mutationCoverageCommandData: MutationCoverageCommandData) {
        MavenService.runMavenCommand(
            project,
            mutationCoverageCommandData.module,
            listOf("test-compile", "pitest:mutationCoverage"),
            MavenService.buildPitestArgs(mutationCoverageCommandData)
        )
    }
}