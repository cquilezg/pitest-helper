package com.cquilez.pitesthelper.application.usecase

import com.cquilez.pitesthelper.application.cqrs.command.RunMutationCoverageFromProjectViewCommand
import com.cquilez.pitesthelper.application.port.`in`.RunMutationCoverageFromProjectViewPort
import com.cquilez.pitesthelper.application.port.out.BuildUnitPort
import com.cquilez.pitesthelper.application.port.out.CodeElementPort
import com.cquilez.pitesthelper.application.port.out.SourceFolderPort
import com.cquilez.pitesthelper.application.port.out.UserInterfacePort
import com.cquilez.pitesthelper.domain.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class RunMutationCoverageFromProjectViewUseCase(val project: Project) : RunMutationCoverageFromProjectViewPort {

    private val buildUnitPort = project.service<BuildUnitPort>()
    private val codeElementPort = project.service<CodeElementPort>()
    private val sourceFolderPort = project.service<SourceFolderPort>()
    private val userInterfacePort = project.service<UserInterfacePort>()

    override fun execute(command: RunMutationCoverageFromProjectViewCommand) {
        val (codeElements, errors) = codeElementPort.findCodeElements(command.nodes)

        val (testCodeElements, productionCodeElements) = codeElements.partition { codeItem ->
            codeItem.sourceFolder.codeType == CodeType.TEST
        }

        val optimizedNormalCodeItems = CodeElement.removeNestedElements(productionCodeElements)
        val optimizedTestCodeItems = CodeElement.removeNestedElements(testCodeElements)

        val allErrors = errors.toMutableList()

        val (foundTestCodeItems, normalErrors) = discoverCodeInOppositeSourceFolders(
            optimizedNormalCodeItems,
            CodeType.TEST
        )
        allErrors.addAll(normalErrors)

        val (foundNormalCodeItems, testErrors) = discoverCodeInOppositeSourceFolders(
            optimizedTestCodeItems,
            CodeType.PRODUCTION
        )
        allErrors.addAll(testErrors)

        // Combine, deduplicate, then remove nested elements again
        val allNormalCodeItems = CodeElement.removeNestedElements(
            (optimizedNormalCodeItems + foundNormalCodeItems).distinctBy { it.qualifiedName }
        )
        val allTestCodeItems = CodeElement.removeNestedElements(
            (optimizedTestCodeItems + foundTestCodeItems).distinctBy { it.qualifiedName }
        )

        showMutationCoverageDialog(allNormalCodeItems, allTestCodeItems, codeElements, allErrors)
    }

    private fun showMutationCoverageDialog(
        allNormalCodeItems: List<CodeElement>,
        allTestCodeItems: List<CodeElement>,
        codeElements: List<CodeElement>,
        allErrors: List<String>
    ) {
        val targetClasses = CodeElement.formatList(allNormalCodeItems)
        val targetTests = CodeElement.formatList(allTestCodeItems)

        val workingUnit = determineWorkingUnit(codeElements)
        if (workingUnit != null) {
            val buildSystem = workingUnit.buildSystem
            val isSubmodule = workingUnit.let { buildUnitPort.findParent(it) != null }
            val allBuildUnits = buildUnitPort.getAllBuildUnits()

            val mutationCoverageOptions = MutationCoverageOptions(
                targetClasses,
                targetTests,
                "",
                "",
                allErrors,
                workingUnit,
                buildSystem,
                isSubmodule,
                allBuildUnits
            )
            userInterfacePort.showMutationCoverageDialog(mutationCoverageOptions)
        }
    }

    private fun discoverCodeInOppositeSourceFolders(
        codeItems: List<CodeElement>,
        targetCodeType: CodeType
    ): Pair<List<CodeElement>, List<String>> {
        val result = mutableListOf<CodeElement>()
        val errors = mutableListOf<String>()

        codeItems.forEach { element ->
            val oppositeSourceFolder = findSourceFolder(element.sourceFolder, targetCodeType)
            if (oppositeSourceFolder == null) {
                val codeTypeLabel = if (targetCodeType == CodeType.TEST) "test" else "main"
                errors.add("$codeTypeLabel source folder not found")
                return@forEach
            }

            val (correspondingElement, error) = findCodeElement(element, oppositeSourceFolder)
            if (correspondingElement != null) {
                result.add(correspondingElement)
            } else if (error != null) {
                errors.add(error)
            }
        }

        return Pair(result, errors)
    }

    private fun findSourceFolder(
        sourceFolder: SourceFolder,
        targetCodeType: CodeType
    ): SourceFolder? {
        val buildUnit = buildUnitPort.findBuildUnit(sourceFolder) ?: return null
        return buildUnit.sourceFolders.firstOrNull {
            it.codeType == targetCodeType
        }
    }

    private fun findCodeElement(
        element: CodeElement,
        sourceFolder: SourceFolder
    ): Pair<CodeElement?, String?> {
        return when (element) {
            is CodePackage -> sourceFolderPort.findPackage(element, sourceFolder)
            is CodeClass -> sourceFolderPort.findClass(element, sourceFolder)
            else -> Pair(null, null)
        }
    }

    private fun determineWorkingUnit(codeElements: List<CodeElement>): BuildUnit? {
        if (codeElements.isEmpty()) return null

        val buildUnits = codeElements.mapNotNull { buildUnitPort.findBuildUnit(it.sourceFolder) }.distinct()

        if (buildUnits.size == 1) {
            return buildUnits.first()
        }

        return buildUnits.maxByOrNull { buildUnit ->
            val depth = buildUnit.buildPath.parent.nameCount
            val isChild = buildUnitPort.findParent(buildUnit) != null
            (if (isChild) 10000 else 0) + depth
        }
    }
}
