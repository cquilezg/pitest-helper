package com.cquilez.pitesthelper.services

import com.cquilez.pitesthelper.MyBundle
import com.cquilez.pitesthelper.exception.PitestHelperException
import com.cquilez.pitesthelper.model.BuildSystem
import com.cquilez.pitesthelper.processors.GradleMutationCoverageCommandProcessor
import com.cquilez.pitesthelper.processors.MavenMutationCoverageCommandProcessor
import com.cquilez.pitesthelper.processors.MutationCoverageCommandProcessor
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.idea.maven.utils.MavenUtil
import org.jetbrains.plugins.gradle.util.GradleUtil


@Service(Service.Level.PROJECT)
class BuildSystemService(project: Project) {

    private val helpMessage =
        "Please select only Java classes, packages or a module folder containing Java source code."

    init {
        thisLogger().info(MyBundle.message("buildSystemService", project.name))
    }

    private fun getBuildSystem(
        project: Project,
        navigatableArray: Array<Navigatable>?,
        psiFile: PsiFile?
    ): BuildSystem {
        return if (!navigatableArray.isNullOrEmpty()) {
            getBuildSystemForNavigatables(project, navigatableArray)
        } else if (psiFile != null) {
            getBuildSystemForPsiFile(psiFile)
        } else {
            BuildSystem.OTHER
        }
    }

    private fun getBuildSystemForNavigatables(project: Project, navigatableArray: Array<Navigatable>): BuildSystem {
        return if (navigatableArray.all { MavenUtil.isMavenModule(getModuleForNavigatable(project, it)) }) {
            BuildSystem.MAVEN
        } else if (navigatableArray.all {
                GradleUtil.findGradleModuleData(
                    getModuleForNavigatable(
                        project,
                        it
                    )
                ) != null
            }) {
            BuildSystem.GRADLE
        } else {
            BuildSystem.OTHER
        }
    }

    private fun getBuildSystemForPsiFile(psiFile: PsiFile): BuildSystem {
        return if (MavenUtil.isMavenModule(getModuleFromElement(psiFile))) {
            BuildSystem.MAVEN
        } else if (GradleUtil.findGradleModuleData(getModuleFromElement(psiFile)) != null) {
            BuildSystem.GRADLE
        } else {
            BuildSystem.OTHER
        }
    }

    private fun getModuleForNavigatable(project: Project, navigatable: Navigatable): Module {
        val module = when (navigatable) {
            is ClassTreeNode -> {
                ModuleUtilCore.findModuleForFile((navigatable).virtualFile!!, project)
            }

            is PsiDirectoryNode -> {
                ModuleUtilCore.findModuleForFile((navigatable.value).virtualFile, project)
            }

            else -> {
                null
            }
        }
            ?: throw PitestHelperException("There is/are elements not supported. $helpMessage")
        return module
    }

    private fun getModuleFromElement(psiElement: PsiElement): Module =
        ModuleUtil.findModuleForPsiElement(psiElement)
            ?: throw PitestHelperException("Module was not found!")

    /**
     * Returns the build system processor for the project
     */
    fun getCommandBuilder(
        project: Project,
        projectService: MyProjectService,
        classService: ClassService,
        navigatableArray: Array<Navigatable>?,
        psiFile: PsiFile?
    ): MutationCoverageCommandProcessor {
        return if (!ApplicationManager.getApplication().isUnitTestMode) {
            when (getBuildSystem(project, navigatableArray, psiFile)) {
                BuildSystem.GRADLE -> {
                    GradleMutationCoverageCommandProcessor(
                        project,
                        projectService,
                        classService,
                        navigatableArray,
                        psiFile
                    )
                }

                BuildSystem.MAVEN -> {
                    MavenMutationCoverageCommandProcessor(
                        project,
                        projectService,
                        classService,
                        navigatableArray,
                        psiFile
                    )
                }

                else -> throw PitestHelperException("Unsupported build system. PITest Helper supports only Gradle and Maven projects.")
            }
        } else {
            MavenMutationCoverageCommandProcessor(project, projectService, classService, navigatableArray, psiFile)
        }
    }
}
