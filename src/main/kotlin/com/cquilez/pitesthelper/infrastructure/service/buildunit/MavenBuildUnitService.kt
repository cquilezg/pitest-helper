package com.cquilez.pitesthelper.infrastructure.service.buildunit

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.BuildUnit
import com.cquilez.pitesthelper.domain.CodeType
import com.cquilez.pitesthelper.domain.SourceFolder
import com.cquilez.pitesthelper.infrastructure.adapter.buildunit.AbstractBuildUnitServiceAdapter
import com.cquilez.pitesthelper.infrastructure.service.sourcefolder.SourceFolderService
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootManager
import kotlinx.collections.immutable.toImmutableSet
import org.jetbrains.jps.model.java.JavaSourceRootType
import java.nio.file.Path
import kotlin.io.path.Path

private data class BuildFileInfo(
    val buildSystem: BuildSystem,
    val buildPath: Path,
    val buildFileName: String,
    val sourceFolders: List<SourceFolder>
)

class MavenBuildUnitService : AbstractBuildUnitServiceAdapter() {

    override fun getBuildSystem(): BuildSystem = BuildSystem.MAVEN

    override fun scanBuildUnits(project: Project): List<BuildUnit> {
        val sourceFolderService = project.service<SourceFolderService>()
        val buildFileInfoList = detectAllBuildFiles(project, sourceFolderService)
        return buildHierarchyFromBottomToTop(buildFileInfoList)
    }

    private fun detectAllBuildFiles(project: Project, sourceFolderService: SourceFolderService): List<BuildFileInfo> {
        val modules = ModuleManager.getInstance(project).modules

        val buildFileLocations =
            mutableMapOf<Path, Triple<Path, String, Module>>() // buildUnitDir -> (buildFilePath, buildFileName, module)

        for (module in modules) {
            val contentRoots = ModuleRootManager.getInstance(module).contentRoots

            for (contentRoot in contentRoots) {
                val pomFile = contentRoot.findChild("pom.xml")
                if (pomFile != null && pomFile.exists()) {
                    val pomPath = Path(pomFile.path)
                    val buildUnitDir = pomPath.parent
                    buildFileLocations[buildUnitDir] = Triple(pomPath, "pom.xml", module)
                }
            }
        }

        val allBuildUnitDirs = buildFileLocations.keys.toImmutableSet()
        return buildFileLocations.map { (buildUnitDir, buildFileInfo) ->
            val (buildFilePath, buildFileName, module) = buildFileInfo
            val nestedBuildUnitDirs = allBuildUnitDirs.filter { otherDir ->
                otherDir != buildUnitDir && otherDir.startsWith(buildUnitDir)
            }.toSet()

            val sourceFolders =
                findSourceFoldersInModule(module, buildUnitDir, nestedBuildUnitDirs, sourceFolderService)
            BuildFileInfo(BuildSystem.MAVEN, buildFilePath, buildFileName, sourceFolders)
        }
    }

    private fun buildHierarchyFromBottomToTop(buildFileInfoList: List<BuildFileInfo>): List<BuildUnit> {
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

        // Return only root build units (those that are not children of any other build unit)
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

    private fun findSourceFoldersInModule(
        module: Module,
        buildUnitDir: Path,
        nestedBuildUnitDirs: Set<Path>,
        sourceFolderService: SourceFolderService
    ): List<SourceFolder> {
        val sourceFolders = mutableListOf<SourceFolder>()

        module.rootManager.contentEntries.forEach { contentEntry ->
            contentEntry.sourceFolders
                .filter { it.rootType is JavaSourceRootType }
                .filter { !sourceFolderService.isAutogeneratedSourceFolder(it) }
                .forEach { intellijSourceFolder ->
                    val virtualFile = intellijSourceFolder.file
                    if (virtualFile != null) {
                        val sourceFolderPath = Path(virtualFile.path)

                        // Include source folder only if it's within this build unit
                        // but NOT within any nested build unit
                        val isWithinBuildUnit = sourceFolderPath.startsWith(buildUnitDir)
                        val isWithinNestedBuildUnit = nestedBuildUnitDirs.any { sourceFolderPath.startsWith(it) }

                        if (isWithinBuildUnit && !isWithinNestedBuildUnit) {
                            val codeType = if (intellijSourceFolder.rootType.isForTests) {
                                CodeType.TEST
                            } else {
                                CodeType.PRODUCTION
                            }

                            sourceFolders.add(
                                SourceFolder(
                                    path = sourceFolderPath,
                                    codeType = codeType
                                )
                            )
                        }
                    }
                }
        }

        return sourceFolders.distinct()
    }
}

