package com.cquilez.pitesthelper.application.usecase

import com.cquilez.pitesthelper.application.cqrs.command.RunMutationCoverageFromProjectViewCommand
import com.cquilez.pitesthelper.application.port.`in`.RunMutationCoverageFromProjectViewPort
import com.cquilez.pitesthelper.application.port.out.CodeElementPort
import com.cquilez.pitesthelper.application.port.out.ProjectConfigPort
import com.cquilez.pitesthelper.application.port.out.UserInterfacePort
import com.cquilez.pitesthelper.domain.BuildUnit
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.domain.model.*
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache

class RunMutationCoverageFromProjectViewUseCase(val project: Project) : RunMutationCoverageFromProjectViewPort {

    private val codeElementPort = project.service<CodeElementPort>()
    private val projectConfigPort = project.service<ProjectConfigPort>()
    private val userInterfacePort = project.service<UserInterfacePort>()
    private val psiManager = PsiManager.getInstance(project)
    private val localFileSystem = LocalFileSystem.getInstance()
    private val javaDirectoryService = JavaDirectoryService.getInstance()

    override fun execute(command: RunMutationCoverageFromProjectViewCommand) {
        val (codeElements, errors) = codeElementPort.getCodeElements(command.nodes)

        val projectSettings = projectConfigPort.getDefaultSettings()

        val (testCodeItems, normalCodeItems) = codeElements.partition { codeItem ->
            codeItem.sourceFolder.codeType == CodeType.TEST
        }

        val optimizedNormalCodeItems = removeNestedElements(normalCodeItems)
        val optimizedTestCodeItems = removeNestedElements(testCodeItems)

        val allErrors = errors.toMutableList()

        val (enhancedNormalCodeItems, normalErrors) = discoverCorrespondingElements(
            optimizedNormalCodeItems,
            CodeType.TEST
        )
        allErrors.addAll(normalErrors)

        val (enhancedTestCodeItems, testErrors) = discoverCorrespondingElements(
            optimizedTestCodeItems,
            CodeType.PRODUCTION
        )
        allErrors.addAll(testErrors)

        val targetClasses = formatCodeItems(enhancedNormalCodeItems)
        val targetTests = formatCodeItems(enhancedTestCodeItems)

        val workingUnit = determineWorkingUnit(codeElements)

        val mutationCoverageOptions = MutationCoverageOptions(
            module = null, //TODO: extract module from projectElements
            preActions = projectSettings.preTasks,
            postActions = projectSettings.postTasks,
            targetClasses = targetClasses,
            targetTests = targetTests,
            errors = allErrors,
            workingUnit = workingUnit
        )
        userInterfacePort.showMutationCoverageDialog(mutationCoverageOptions)
    }

    private fun removeNestedElements(codeItems: List<CodeElement>): List<CodeElement> {
        val packages = codeItems.filterIsInstance<CodePackage>()
        val classes = codeItems.filterIsInstance<CodeClass>()

        // Filter out packages that are nested within other packages
        val optimizedPackages = packages.filter { pkg ->
            packages.none { other ->
                other != pkg && pkg.qualifiedName.startsWith("${other.qualifiedName}.")
            }
        }

        // Filter out classes that are covered by any package
        val optimizedClasses = classes.filter { cls ->
            optimizedPackages.none { pkg ->
                cls.qualifiedName.startsWith("${pkg.qualifiedName}.")
            }
        }

        return optimizedPackages + optimizedClasses
    }

    private fun discoverCorrespondingElements(
        codeItems: List<CodeElement>,
        targetCodeType: CodeType
    ): Pair<List<CodeElement>, List<String>> {
        val result = codeItems.toMutableList()
        val errors = mutableListOf<String>()

        codeItems.forEach { element ->
            val oppositeSourceFolder = findOppositeSourceFolder(element.sourceFolder, targetCodeType)

            if (oppositeSourceFolder == null) {
                // No opposite source folder exists in this BuildUnit (might be legitimate)
                return@forEach
            }

            val (correspondingElement, error) = findCorrespondingElement(element, oppositeSourceFolder)

            if (correspondingElement != null) {
                result.add(correspondingElement)
            } else if (error != null) {
                errors.add(error)
            }
        }

        return Pair(result, errors)
    }

    private fun findOppositeSourceFolder(
        sourceFolder: SourceFolder,
        targetCodeType: CodeType
    ): SourceFolder? {
        return sourceFolder.buildUnit.sourceFolders.firstOrNull {
            it.codeType == targetCodeType
        }
    }

    private fun findCorrespondingElement(
        element: CodeElement,
        oppositeSourceFolder: SourceFolder
    ): Pair<CodeElement?, String?> {
        return when (element) {
            is CodePackage -> findCorrespondingPackage(element, oppositeSourceFolder)
            is CodeClass -> findCorrespondingClass(element, oppositeSourceFolder)
            else -> Pair(null, null)
        }
    }

    private fun findCorrespondingPackage(
        pkg: CodePackage,
        oppositeSourceFolder: SourceFolder
    ): Pair<CodeElement?, String?> {
        val sourceVirtualFile = localFileSystem.findFileByPath(oppositeSourceFolder.path.toString())
            ?: return createPackageError(oppositeSourceFolder, pkg)

        val packagePath = pkg.qualifiedName.replace('.', '/')
        val packageDirectory = sourceVirtualFile.findFileByRelativePath(packagePath)

        if (packageDirectory != null && packageDirectory.isDirectory) {
            val psiDirectory = psiManager.findDirectory(packageDirectory)
            if (psiDirectory != null) {
                val psiPackage = javaDirectoryService.getPackage(psiDirectory)
                if (psiPackage != null && psiPackage.qualifiedName == pkg.qualifiedName) {
                    val correspondingPackage = CodePackage(
                        path = packageDirectory.toNioPath(),
                        qualifiedName = pkg.qualifiedName,
                        sourceFolder = oppositeSourceFolder
                    )
                    return Pair(correspondingPackage, null)
                }
            }
        }

        return createPackageError(oppositeSourceFolder, pkg)
    }

    private fun createPackageError(oppositeSourceFolder: SourceFolder, pkg: CodePackage): Pair<CodeElement?, String> {
        val codeTypeLabel = if (oppositeSourceFolder.codeType == CodeType.TEST) "test" else "production"
        return Pair(null, "Package ${pkg.qualifiedName} not found in $codeTypeLabel source folder")
    }

    private fun findCorrespondingClass(
        cls: CodeClass,
        oppositeSourceFolder: SourceFolder
    ): Pair<CodeElement?, String?> {
        val targetSimpleName: String
        val targetQualifiedName: String

        if (oppositeSourceFolder.codeType == CodeType.TEST) {
            targetSimpleName = "${cls.simpleName}Test"
            targetQualifiedName = "${cls.qualifiedName}Test"
        } else {
            if (cls.simpleName.endsWith("Test")) {
                targetSimpleName = cls.simpleName.removeSuffix("Test")
                targetQualifiedName = cls.qualifiedName.removeSuffix("Test")
            } else {
                return Pair(null, null)
            }
        }

        val sourceVirtualFile = localFileSystem.findFileByPath(oppositeSourceFolder.path.toString())
            ?: return createClassError(oppositeSourceFolder, targetQualifiedName)

        val module = ModuleUtilCore.findModuleForFile(sourceVirtualFile, project)
            ?: return createClassError(oppositeSourceFolder, targetQualifiedName)

        val psiClasses = PsiShortNamesCache.getInstance(project)
            .getClassesByName(targetSimpleName, GlobalSearchScope.moduleScope(module))

        val psiClass = psiClasses.firstOrNull { it.qualifiedName == targetQualifiedName }

        return if (psiClass != null) {
            val classFile = psiClass.containingFile?.virtualFile
            if (classFile != null) {
                val correspondingClass = CodeClass(
                    path = classFile.toNioPath(),
                    qualifiedName = targetQualifiedName,
                    simpleName = targetSimpleName,
                    sourceFolder = oppositeSourceFolder
                )
                Pair(correspondingClass, null)
            } else {
                createClassError(oppositeSourceFolder, targetQualifiedName)
            }
        } else {
            createClassError(oppositeSourceFolder, targetQualifiedName)
        }
    }

    private fun createClassError(
        oppositeSourceFolder: SourceFolder,
        targetQualifiedName: String
    ): Pair<CodeElement?, String> {
        val codeTypeLabel = if (oppositeSourceFolder.codeType == CodeType.TEST) "test" else "production"
        return Pair(null, "Class $targetQualifiedName not found in $codeTypeLabel source folder")
    }

    private fun formatCodeItems(codeItems: List<CodeElement>): String {
        return codeItems
            .map { codeItem ->
                when (codeItem) {
                    is CodePackage -> "${codeItem.qualifiedName}.*"
                    else -> codeItem.qualifiedName
                }
            }
            .sorted()
            .joinToString(",")
    }

    private fun determineWorkingUnit(codeElements: List<CodeElement>): BuildUnit? {
        if (codeElements.isEmpty()) return null

        val buildUnits = codeElements.map { it.sourceFolder.buildUnit }.distinct()

        if (buildUnits.size == 1) {
            return buildUnits.first()
        }

        return buildUnits.maxByOrNull { buildUnit ->
            val depth = buildUnit.buildPath.parent.nameCount
            val isChild = buildUnit.parent != null
            (if (isChild) 10000 else 0) + depth
        }
    }
}
