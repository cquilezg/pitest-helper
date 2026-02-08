package com.cquilez.pitesthelper.infrastructure.adapter.buildunit

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.BuildUnit
import com.cquilez.pitesthelper.domain.SourceFolder
import com.cquilez.pitesthelper.infrastructure.extension.BuildUnitExtensionPoint
import com.intellij.openapi.extensions.ExtensionPointName
import java.nio.file.Path

abstract class AbstractBuildUnitExtensionPoint : BuildUnitExtensionPoint {

    protected data class BuildFileInfo(
        val buildSystem: BuildSystem,
        val buildPath: Path,
        val buildFileName: String,
        val sourceFolders: List<SourceFolder>
    )

    companion object {
        val EP_NAME: ExtensionPointName<BuildUnitExtensionPoint> =
            ExtensionPointName.create("com.cquilez.pitesthelper.buildUnitExtensionPoint")

        fun forBuildSystem(buildSystem: BuildSystem): BuildUnitExtensionPoint? {
            return EP_NAME.extensionList.find { it.getBuildSystem() == buildSystem }
        }
    }

    protected fun buildHierarchyFromBottomToTop(buildFileInfoList: List<BuildFileInfo>): List<BuildUnit> {
        if (buildFileInfoList.isEmpty()) return emptyList()

        val sortedByDepthDescending = buildFileInfoList.sortedByDescending { info ->
            info.buildPath.parent.nameCount
        }

        val childrenMap = buildChildrenMap(sortedByDepthDescending)

        val childBuildPaths = childrenMap.values.flatten().map { it.buildPath }.toSet()

        val buildUnitMap = mutableMapOf<Path, BuildUnit>()

        for (buildFileInfo in sortedByDepthDescending) {
            val childBuildUnits = childrenMap[buildFileInfo.buildPath]
                ?.mapNotNull { buildUnitMap[it.buildPath] }
                ?: emptyList()

            val buildUnit = BuildUnit(
                name = buildFileInfo.buildPath.parent.fileName.toString(),
                buildSystem = buildFileInfo.buildSystem,
                buildPath = buildFileInfo.buildPath,
                sourceFolders = buildFileInfo.sourceFolders,
                buildUnits = childBuildUnits
            )

            buildUnitMap[buildFileInfo.buildPath] = buildUnit
        }

        return sortedByDepthDescending
            .filter { it.buildPath !in childBuildPaths }
            .mapNotNull { buildUnitMap[it.buildPath] }
    }

    private fun buildChildrenMap(sortedByDepthDescending: List<BuildFileInfo>): Map<Path, MutableList<BuildFileInfo>> {
        val childrenMap = mutableMapOf<Path, MutableList<BuildFileInfo>>()

        for (child in sortedByDepthDescending) {
            val childDir = child.buildPath.parent
            var immediateParent: BuildFileInfo? = null

            for (potentialParent in sortedByDepthDescending) {
                if (potentialParent == child) continue
                val parentDir = potentialParent.buildPath.parent

                if (childDir.startsWith(parentDir) && childDir != parentDir
                    && (immediateParent == null || immediateParent.buildPath.parent.nameCount < parentDir.nameCount)
                ) {
                    immediateParent = potentialParent
                }
            }

            if (immediateParent != null) {
                childrenMap.getOrPut(immediateParent.buildPath) { mutableListOf() }.add(child)
            }
        }
        return childrenMap
    }
}