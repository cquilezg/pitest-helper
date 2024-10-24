package com.cquilez.pitesthelper.services

import com.cquilez.pitesthelper.exception.PitestHelperException
import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.processors.GradleMutationCoverageCommandProcessor
import com.cquilez.pitesthelper.processors.MavenMutationCoverageCommandProcessor
import com.cquilez.pitesthelper.processors.MutationCoverageCommandProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.idea.maven.utils.MavenUtil
import org.jetbrains.plugins.gradle.util.GradleUtil

@Service(Service.Level.PROJECT)
class BuildSystemService {

    private fun getBuildSystem(
        project: Project,
        projectService: MyProjectService,
        extensionsService: ExtensionsService,
        navigatableArray: Array<Navigatable>?,
        psiFile: PsiFile?
    ): BuildSystem {
        return if (!navigatableArray.isNullOrEmpty()) {
            getBuildSystemForNavigatables(project, projectService, extensionsService, navigatableArray)
        } else if (psiFile != null) {
            getBuildSystemForPsiFile(psiFile)
        } else {
            BuildSystem.OTHER
        }
    }

    private fun getBuildSystemForNavigatables(
        project: Project,
        projectService: MyProjectService,
        extensionsService: ExtensionsService,
        navigatableArray: Array<Navigatable>
    ): BuildSystem {
        return if (navigatableArray.all {
                MavenUtil.isMavenModule(
                    projectService.findNavigatableModule(
                        project,
                        extensionsService,
                        it
                    )
                )
            }) {
            BuildSystem.MAVEN
        } else if (navigatableArray.all {
                GradleUtil.findGradleModuleData(
                    projectService.findNavigatableModule(
                        project,
                        extensionsService,
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
        extensionsService: ExtensionsService,
        navigatableArray: Array<Navigatable>?,
        psiFile: PsiFile?
    ): MutationCoverageCommandProcessor {
        return if (!ApplicationManager.getApplication().isUnitTestMode) {
            when (getBuildSystem(project, projectService, extensionsService, navigatableArray, psiFile)) {
                BuildSystem.GRADLE -> {
                    GradleMutationCoverageCommandProcessor(
                        project,
                        projectService,
                        classService,
                        extensionsService,
                        navigatableArray,
                        psiFile
                    )
                }

                BuildSystem.MAVEN -> {
                    MavenMutationCoverageCommandProcessor(
                        project,
                        projectService,
                        classService,
                        extensionsService,
                        navigatableArray,
                        psiFile
                    )
                }

                else -> throw PitestHelperException("Unsupported build system. PITest Helper supports only Gradle and Maven projects.")
            }
        } else {
            MavenMutationCoverageCommandProcessor(project, projectService, classService, extensionsService, navigatableArray, psiFile)
        }
    }
}
