package com.cquilez.pitesthelper.infrastructure.service

import com.cquilez.pitesthelper.application.port.out.FileDataExtractorFromUiOutPort
import com.cquilez.pitesthelper.domain.*
import com.cquilez.pitesthelper.exception.PitestHelperException
import com.cquilez.pitesthelper.infrastructure.dto.ProjectAnalysisRequestDTO
import com.cquilez.pitesthelper.infrastructure.dto.ProjectElementDataDTO
import com.cquilez.pitesthelper.model.MutationCoverageData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.model.project.ContentRootData.SourceRoot
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.util.containers.stream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name

@Service
class FileDataExtractorFromUIService : FileDataExtractorFromUiOutPort<AnActionEvent, ProjectAnalysisRequestDTO> {

    override fun processObjects(projectElements: List<ProjectElement>): MutationCoverageData {
        TODO("Not yet implemented")
    }

    override fun extractAnalysisRequestFromUI(inputData: AnActionEvent): ProjectAnalysisRequestDTO {
        val navigatables = inputData.getData(CommonDataKeys.NAVIGATABLE_ARRAY)
        return ProjectAnalysisRequestDTO(
            inputData.project!!,
            navigatables.stream().map {
                when (val projectViewNodeValue = (it as ProjectViewNode<*>).value) {
                    is NavigatablePsiElement -> {
                        extractPsiFileData(projectViewNodeValue)
                    }

                    else -> {
                        throw PitestHelperException("No project file detected for: $projectViewNodeValue")
                    }
                }
            }.toList()
        )
    }

    private fun extractPsiFileData(navigatablePsiElement: NavigatablePsiElement): ProjectElementDataDTO {
        if (navigatablePsiElement is PsiDirectory) {
            val virtualFile = navigatablePsiElement.virtualFile
            return ProjectElementDataDTO(
                navigatablePsiElement, findModule(virtualFile, project),
                navigatablePsiElement.virtualFile, findPsiDirectorySourceRoot(navigatablePsiElement)
            )
        } else {
            navigatablePsiElement.containingFile.virtualFile
            throw PitestHelperException("No project file detected for: ${navigatablePsiElement.name}")
        }
        val module = if (navigatablePsiElement is PsiDirectory) {

        }
        val sourceRoot = if (navigatablePsiElement is PsiDirectory) {

        }
        return ProjectElementDataDTO(navigatablePsiElement, module, virtualFile, sourceRoot)
    }

    private fun findModule(virtualFile: VirtualFile, project: Project): com.intellij.openapi.module.Module {
        return ModuleUtilCore.findModuleForFile(virtualFile, project)
            ?: throw PitestHelperException("Module not found!")
    }

    private fun findPsiDirectorySourceRoot(psiDirectory: PsiDirectory): SourceRoot {

    }

    // TODO: get virtual files previously on
    override fun extractSelectedProjectFiles(inputData: AnActionEvent): List<ProjectElement> {
        val navigatables = inputData.getData(CommonDataKeys.NAVIGATABLE_ARRAY)
        val project = inputData.project!!
        val projectPath = Path(project.basePath!!)
        val buildUnits = mutableListOf<BuildUnit>()
        val list: List<ProjectElement> = navigatables.stream().map {
            when (val psiValue = (it as ProjectViewNode<*>).value) {
                is PsiDirectory -> {
                    val filePath = Path(psiValue.virtualFile.path)
                    ProjectDirectory(filePath, findBuildUnit(projectPath, buildUnits, filePath))
                }

                is PsiElement -> {
                    val filePath = Path(psiValue.containingFile.virtualFile.path)
                    ProjectFile(filePath, findBuildUnit(projectPath.parent, buildUnits, filePath))
                }

                else -> {
                    throw PitestHelperException("No project file detected for: $psiValue")
                }
            }
        }.toList()
        return list
    }

    override fun extractProjectElementsFromRequestData(projectContext: ProjectAnalysisRequestDTO): List<ProjectElement> {
        val projectPath = Path(projectContext.project.basePath!!)
        val buildUnits = mutableListOf<BuildUnit>()
        val list: List<ProjectElement> = projectContext.selectedItems.stream().map {
            when (it) {
                is PsiDirectory -> {
                    val filePath = Path(it.virtualFile.path)
                    ProjectDirectory(filePath, findBuildUnit(projectPath, buildUnits, filePath))
                }

                is PsiElement -> {
                    val filePath = Path(it.containingFile.virtualFile.path)
                    ProjectFile(filePath, findBuildUnit(projectPath.parent, buildUnits, filePath))
                }

                else -> {
                    throw PitestHelperException("No project file detected for: $it")
                }
            }
        }.toList()
        return list
    }

    private fun findBuildUnit(projectPath: Path, buildUnits: MutableList<BuildUnit>, filePath: Path): BuildUnit {
        val buildUnit = findExistingBuildUnit(buildUnits, filePath)
        if (buildUnit != null) {
            return buildUnit
        }

        var buildFile = findBuildFilePath(projectPath, filePath, listOf("pom.xml"))
        if (buildFile != null) {
            return saveBuildUnit(buildUnits, BuildSystem.MAVEN, buildFile)
        }
        buildFile = findBuildFilePath(
            projectPath,
            filePath,
            listOf("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts")
        )
        if (buildFile != null) {
            return saveBuildUnit(buildUnits, BuildSystem.GRADLE, buildFile)
        }
        throw PitestHelperException("No build unit found for file: $filePath")
    }

    private fun saveBuildUnit(
        buildUnits: MutableList<BuildUnit>,
        buildSystem: BuildSystem,
        buildFilePath: Path
    ): BuildUnit {
        val buildUnit = BuildUnit(buildSystem, buildFilePath, buildFilePath.name)
        buildUnits.add(buildUnit)
        return buildUnit
    }

    private fun findBuildFilePath(basePath: Path, currentPath: Path, fileNames: List<String>): Path? {
        for (fileName in fileNames) {
            val filePath = currentPath.resolve(fileName)
            if (Files.exists(filePath)) {
                return filePath
            }
        }
        if (currentPath == basePath) {
            return null
        }
        return findBuildFilePath(basePath, currentPath.parent, fileNames)
    }

    private fun findExistingBuildUnit(buildUnits: MutableList<BuildUnit>, filePath: Path): BuildUnit? {
        for (buildUnit in buildUnits) {
            if (filePath.startsWith(buildUnit.buildPath.parent)) {
                return buildUnit
            }
        }
        return null
    }
}
