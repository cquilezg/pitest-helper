package com.cquilez.pitesthelper.processors

import com.cquilez.pitesthelper.exception.PitestHelperException
import com.cquilez.pitesthelper.services.LanguageProcessorService
import com.cquilez.pitesthelper.model.MutationCoverageCommandData
import com.cquilez.pitesthelper.services.ClassService
import com.cquilez.pitesthelper.services.GradleService
import com.cquilez.pitesthelper.services.MyProjectService
import com.cquilez.pitesthelper.services.ServiceProvider
import com.cquilez.pitesthelper.services.persistence.PitestConfigService
import com.intellij.openapi.components.service
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

    private var gradleModules: Set<String> = setOf()
    private var moduleName = ""

    override fun resolveModules() {
        checkAllElementsAreInSameModule()
        resolveModules(resolveWorkingModule())
        resolveGradleModules()
    }

    override fun buildCommand(mutationCoverageCommandData: MutationCoverageCommandData) =
        let {
            val tasks = buildTaskList(mutationCoverageCommandData).joinToString(" ")
            "gradle $tasks ${buildPitestArgs(mutationCoverageCommandData)}"
        }


    override fun runCommand(mutationCoverageCommandData: MutationCoverageCommandData) {
        GradleService.runCommand(project, "${buildTaskList(mutationCoverageCommandData)} ${buildPitestArgs(mutationCoverageCommandData)}")
    }

    override fun saveSettings(mutationCoverageCommandData: MutationCoverageCommandData) {
        val serviceProvider = project.service<ServiceProvider>()
        val pitestConfigService = serviceProvider.getService<PitestConfigService>(project)
        pitestConfigService.preGoals = mutationCoverageCommandData.preActions
        pitestConfigService.postGoals = mutationCoverageCommandData.postActions
    }

    override fun checkAllElementsAreInSameModule() {
        if (navigatableArray != null) {
            if (navigatableArray.isEmpty()) {
                throw PitestHelperException("There are no elements to process")
            }
            var module: DataNode<ModuleData>? = null
            navigatableArray.forEach {
                val newModule = GradleUtil.findGradleModuleData(projectService.findNavigatableModule(project, languageProcessorService, it))
                if (module == null) {
                    module = newModule
                } else if (module != newModule) {
                    throw PitestHelperException("You cannot choose elements from different modules")
                }
            }
        }
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
                        ((isTestModule && projectService.isMainSourceFolder(it)) || (!isTestModule && projectService.isTestSourceFolder(
                            it
                        )))
                    }.isEmpty()
                }
            }.toList()
    }

    private fun getModuleBasePath(module: Module): String {
        if (module.rootManager.contentEntries.isEmpty()) {
            return ""
        }
        moduleName = extractModuleName(module.name)
        val basePath = module.rootManager.contentEntries[0].url
        if(moduleName.isNotBlank()) {
            val moduleSubPath = "/$moduleName/"
            return basePath.substringBeforeLast(moduleSubPath) + moduleSubPath
        }
        return "$basePath/"
    }

    private fun buildPitestGoal(): String {
        val pitestGoal = if (gradleModules.size > 1 && moduleName.isNotBlank()) {
            ":${moduleName}:pitest"
        } else {
            "pitest"
        }
        return pitestGoal
    }

    private fun resolveWorkingModule(): Module {
        return if (psiFile != null) {
            projectService.getModuleFromElement(psiFile)
        } else if (!navigatableArray.isNullOrEmpty()) {
            projectService.findNavigatableModule(project, languageProcessorService, navigatableArray[0])
        } else {
            throw PitestHelperException("No elements to process")
        }
    }

    private fun buildPitestArgs(mutationCoverageCommandData: MutationCoverageCommandData) =
        "-Ppitest.targetClasses=${mutationCoverageCommandData.targetClasses} -Ppitest.targetTests=${mutationCoverageCommandData.targetTests}"

    private fun resolveGradleModules() {
        gradleModules = ModuleManager.getInstance(project).modules.stream()
            .filter { it.name.contains(".") }
            .map {
                extractModuleName(it.name)
            }.toList().toSet()
    }

    private fun buildTaskList(mutationCoverageCommandData: MutationCoverageCommandData) =
        buildActions(
            mutationCoverageCommandData.preActions, buildPitestGoal(),
            mutationCoverageCommandData.postActions
        )

    private fun extractModuleName(moduleName: String): String {
        val moduleChunks = moduleName.split(".")
        return if(moduleChunks.size > 1) {
            moduleChunks[moduleChunks.lastIndex - 1]
        } else {
            ""
        }
    }
}