package com.cquilez.pitesthelper.application.usecase

import com.cquilez.pitesthelper.application.cqrs.command.RunMutationCoverageFromProjectViewCommand
import com.cquilez.pitesthelper.application.port.`in`.RunMutationCoverageFromProjectViewPort
import com.cquilez.pitesthelper.application.port.out.BuildUnitPort
import com.cquilez.pitesthelper.application.port.out.CodeElementPort
import com.cquilez.pitesthelper.application.port.out.SourceFolderPort
import com.cquilez.pitesthelper.application.port.out.UserInterfacePort
import com.cquilez.pitesthelper.domain.BuildUnit
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.domain.model.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class RunMutationCoverageFromProjectViewUseCase(val project: Project) : RunMutationCoverageFromProjectViewPort {

    private val buildUnitPort = project.service<BuildUnitPort>()
    private val codeElementPort = project.service<CodeElementPort>()
    private val sourceFolderPort = project.service<SourceFolderPort>()
    private val userInterfacePort = project.service<UserInterfacePort>()

    override fun execute(command: RunMutationCoverageFromProjectViewCommand) {
        val (codeElements, errors) = codeElementPort.getCodeElements(command.nodes)

        val (testCodeElements, productionCodeElements) = codeElements.partition { codeItem ->
            codeItem.sourceFolder.codeType == CodeType.TEST
        }

        val optimizedNormalCodeItems = codeElementPort.removeNestedElements(productionCodeElements)
        val optimizedTestCodeItems = codeElementPort.removeNestedElements(testCodeElements)

        val allErrors = errors.toMutableList()

        val (foundTestCodeItems, normalErrors) = discoverCorrespondingElements(
            optimizedNormalCodeItems,
            CodeType.TEST
        )
        allErrors.addAll(normalErrors)

        val (findedNormalCodeItems, testErrors) = discoverCorrespondingElements(
            optimizedTestCodeItems,
            CodeType.PRODUCTION
        )
        allErrors.addAll(testErrors)

        // Combine, deduplicate, then remove nested elements again
        val allNormalCodeItems = codeElementPort.removeNestedElements(
            (optimizedNormalCodeItems + findedNormalCodeItems).distinctBy { it.qualifiedName }
        )
        val allTestCodeItems = codeElementPort.removeNestedElements(
            (optimizedTestCodeItems + foundTestCodeItems).distinctBy { it.qualifiedName }
        )

        val targetClasses = formatCodeItems(allNormalCodeItems)
        val targetTests = formatCodeItems(allTestCodeItems)

        val workingUnit = determineWorkingUnit(codeElements)
        val buildSystem = workingUnit?.buildSystem!!
        val isSubmodule = workingUnit?.let { buildUnitPort.findParent(it) != null } ?: false

        val mutationCoverageOptions = MutationCoverageOptions(
            targetClasses,
            targetTests,
            errors,
            workingUnit,
            buildSystem,
            isSubmodule
        )
        userInterfacePort.showMutationCoverageDialog(mutationCoverageOptions)
    }

    private fun discoverCorrespondingElements(
        codeItems: List<CodeElement>,
        targetCodeType: CodeType
    ): Pair<List<CodeElement>, List<String>> {
        val result = mutableListOf<CodeElement>()
        val errors = mutableListOf<String>()

        codeItems.forEach { element ->
            val oppositeSourceFolder = findSourceFolder(element.sourceFolder, targetCodeType)

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

    private fun findSourceFolder(
        sourceFolder: SourceFolder,
        targetCodeType: CodeType
    ): SourceFolder? {
        val buildUnit = buildUnitPort.findBuildUnit(sourceFolder) ?: return null
        return buildUnit.sourceFolders.firstOrNull {
            it.codeType == targetCodeType
        }
    }

    private fun findCorrespondingElement(
        element: CodeElement,
        oppositeSourceFolder: SourceFolder
    ): Pair<CodeElement?, String?> {
        return when (element) {
            is CodePackage -> sourceFolderPort.findCorrespondingPackage(element, oppositeSourceFolder)
            is CodeClass -> sourceFolderPort.findCorrespondingClass(element, oppositeSourceFolder)
            else -> Pair(null, null)
        }
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
