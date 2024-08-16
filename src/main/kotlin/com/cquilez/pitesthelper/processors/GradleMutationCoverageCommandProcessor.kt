package com.cquilez.pitesthelper.processors

import com.cquilez.pitesthelper.exception.PitestHelperException
import com.cquilez.pitesthelper.model.MutationCoverageCommandData
import com.cquilez.pitesthelper.services.ClassService
import com.cquilez.pitesthelper.services.GradleService
import com.cquilez.pitesthelper.services.MyProjectService
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiFile
import com.intellij.util.containers.isEmpty
import com.intellij.util.containers.stream
import org.jetbrains.plugins.gradle.util.GradleUtil

open class GradleMutationCoverageCommandProcessor(
    project: Project,
    projectService: MyProjectService,
    classService: ClassService,
    navigatableArray: Array<Navigatable>?,
    psiFile: PsiFile?
) : MutationCoverageCommandProcessor(
    project,
    projectService,
    classService,
    navigatableArray,
    psiFile
) {

    override fun resolveModules() {
        checkAllElementsAreInSameModule()
        resolveModules(resolveWorkingModule())
    }

    /**
     * In Gradle, there is a module by source folder
     */
    private fun resolveModules(module: Module) {
        val sourceFolders = projectService.getMainSourceFolders(listOf(module))
        if (sourceFolders.isEmpty()) {
            testSourceModules.addAll(listOf(module))
            mainSourceModules.addAll(findOtherModules(module, getModuleBasePath(module), true))
        } else {
            mainSourceModules.addAll(listOf(module))
            testSourceModules.addAll(findOtherModules(module, getModuleBasePath(module), false))
        }
    }

    /**
     * Finds modules which are in the same base path and have the opposite source folder type
     */
    private fun findOtherModules(sourceModule: Module, modulePath: String, isTestModule: Boolean): List<Module> {
        return ModuleManager.getInstance(project).modules.stream()
            .filter { module ->
                module != sourceModule && module.rootManager.contentEntries.any { contentEntry ->
                    contentEntry.url.startsWith(modulePath) && !contentEntry.sourceFolders.stream().filter {
                        ((isTestModule && projectService.isMainSourceFolder(it)) || (!isTestModule && projectService.isTestSourceFolder(it)))
                    }.isEmpty()
                }
            }.toList()
    }

    private fun getModuleBasePath(module: Module): String {
        val moduleChunks = module.name.split(".")
        val moduleName = moduleChunks[moduleChunks.lastIndex - 1]
        return module.rootManager.contentEntries[0].url.substringBefore("/$moduleName/") + "/$moduleName/"
    }

    override fun buildCommand(mutationCoverageCommandData: MutationCoverageCommandData) =
        "./gradlew pitest ${buildPitestArgs(mutationCoverageCommandData)}"

    override fun runCommand(mutationCoverageCommandData: MutationCoverageCommandData) {
        GradleService.runCommand(project, "pitest ${buildPitestArgs(mutationCoverageCommandData)}")
    }

    override fun checkAllElementsAreInSameModule() {
        if (navigatableArray != null) {
            if (navigatableArray.isEmpty()) {
                throw PitestHelperException("There are no elements to process")
            }
            var module: DataNode<ModuleData>? = null
            navigatableArray.forEach {
                val newModule = GradleUtil.findGradleModuleData(projectService.getModuleForNavigatable(project, it))
                if (module == null) {
                    module = newModule
                } else if (module != newModule) {
                    throw PitestHelperException("You cannot choose elements from different modules")
                }
            }
        }
    }

    private fun resolveWorkingModule(): Module {
        return if (psiFile != null) {
            projectService.getModuleFromElement(psiFile)
        } else if (!navigatableArray.isNullOrEmpty()) {
            projectService.getModuleForNavigatable(project, navigatableArray[0])
        } else {
            throw PitestHelperException("No elements to process")
        }
    }

    private fun buildPitestArgs(mutationCoverageCommandData: MutationCoverageCommandData) =
        "-Ppitest.targetClasses=${mutationCoverageCommandData.targetClasses} -Ppitest.targetTests=${mutationCoverageCommandData.targetTests}"
}