package com.cquilez.pitesthelper.infrastructure.adapter

import com.cquilez.pitesthelper.application.port.out.BuildUnitPort
import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.BuildUnit
import com.cquilez.pitesthelper.domain.model.CodeType
import com.cquilez.pitesthelper.domain.model.SourceFolder
import com.cquilez.pitesthelper.infrastructure.service.CacheService
import com.cquilez.pitesthelper.infrastructure.service.SourceFolderService
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jps.model.java.JavaSourceRootType
import java.nio.file.Path
import kotlin.io.path.Path

private data class BuildFileInfo(
    val buildSystem: BuildSystem,
    val buildPath: Path,
    val buildFileName: String,
    val sourceFolders: List<SourceFolder>
)

class BuildUnitAdapter(val project: Project) : BuildUnitPort {

    private val cacheService: CacheService by lazy { project.service<CacheService>() }
    private val sourceFolderService = project.service<SourceFolderService>()

    override fun cleanScanBuildUnits(): List<BuildUnit> {
        cacheService.buildUnitCache.clear()
        cacheService.sourceFolderCache.clear()

        val buildFileInfoList = detectAllBuildFiles()

        val buildUnits = buildHierarchyFromBottomToTop(buildFileInfoList)

        buildUnits.forEach { buildUnit ->
            cacheService.buildUnitCache[buildUnit.buildPath] = buildUnit
            buildUnit.sourceFolders.forEach { sourceFolder ->
                cacheService.sourceFolderCache[sourceFolder.path] = sourceFolder
            }
        }

        return buildUnits
    }

    private fun detectAllBuildFiles(): List<BuildFileInfo> {
        val modules = ModuleManager.getInstance(project).modules
        val buildFileInfoList = mutableListOf<BuildFileInfo>()

        for (module in modules) {
            val contentRoots = ModuleRootManager.getInstance(module).contentRoots

            for (contentRoot in contentRoots) {
                val buildFileInfo = detectBuildFileInModule(contentRoot, module)
                if (buildFileInfo != null) {
                    buildFileInfoList.add(buildFileInfo)
                }
            }
        }

        return buildFileInfoList
    }

    private fun buildHierarchyFromBottomToTop(buildFileInfoList: List<BuildFileInfo>): List<BuildUnit> {
        if (buildFileInfoList.isEmpty()) return emptyList()

        val sortedByDepthDescending = buildFileInfoList.sortedByDescending { info ->
            info.buildPath.parent.nameCount
        }

        val childrenMap = buildChildrenMap(sortedByDepthDescending)

        val buildUnitMap = mutableMapOf<Path, BuildUnit>()

        for (buildFileInfo in sortedByDepthDescending) {
            val childBuildUnits = childrenMap[buildFileInfo.buildPath]
                ?.mapNotNull { buildUnitMap[it.buildPath] }
                ?: emptyList()

            val buildUnit = BuildUnit(
                buildSystem = buildFileInfo.buildSystem,
                buildPath = buildFileInfo.buildPath,
                buildFileName = buildFileInfo.buildFileName,
                sourceFolders = buildFileInfo.sourceFolders,
                buildUnits = childBuildUnits
            )

            buildUnitMap[buildFileInfo.buildPath] = buildUnit
        }

        return sortedByDepthDescending.mapNotNull { buildUnitMap[it.buildPath] }
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

    override fun isPathBuildUnit(path: Path): Boolean {
        return cacheService.getBuildUnitByDirectory(path) != null
    }

    override fun findBuildUnit(sourceFolder: SourceFolder): BuildUnit? {
        return cacheService.buildUnitCache.values.find { buildUnit ->
            buildUnit.sourceFolders.any { it.path == sourceFolder.path }
        }
    }

    override fun findParent(sourceFolder: SourceFolder): BuildUnit? {
        val buildUnit = findBuildUnit(sourceFolder) ?: return null
        return findParent(buildUnit)
    }

    override fun findParent(buildUnit: BuildUnit): BuildUnit? {
        val buildUnitDir = buildUnit.buildPath.parent

        // Look through all cached build units to find the immediate parent
        return cacheService.buildUnitCache.values
            .filter { potentialParent ->
                val parentDir = potentialParent.buildPath.parent
                buildUnitDir.startsWith(parentDir) && buildUnitDir != parentDir
            }
            .maxByOrNull { it.buildPath.parent.nameCount }
    }

    private fun detectBuildFileInModule(contentRoot: VirtualFile, module: Module): BuildFileInfo? {
        val pomFile = contentRoot.findChild("pom.xml")
        if (pomFile != null && pomFile.exists()) {
            val pomPath = Path(pomFile.path)
            val sourceFolders = findSourceFoldersInModule(module, pomPath.parent)
            return BuildFileInfo(BuildSystem.MAVEN, pomPath, "pom.xml", sourceFolders)
        }

        val gradleKtsFile = contentRoot.findChild("build.gradle.kts")
        if (gradleKtsFile != null && gradleKtsFile.exists()) {
            val gradlePath = Path(gradleKtsFile.path)
            val sourceFolders = findSourceFoldersInModule(module, gradlePath.parent)
            return BuildFileInfo(BuildSystem.GRADLE, gradlePath, "build.gradle.kts", sourceFolders)
        }

        val gradleFile = contentRoot.findChild("build.gradle")
        if (gradleFile != null && gradleFile.exists()) {
            val gradlePath = Path(gradleFile.path)
            val sourceFolders = findSourceFoldersInModule(module, gradlePath.parent)
            return BuildFileInfo(BuildSystem.GRADLE, gradlePath, "build.gradle", sourceFolders)
        }

        return null
    }

    private fun findSourceFoldersInModule(module: Module, buildUnitDir: Path): List<SourceFolder> {
        val sourceFolders = mutableListOf<SourceFolder>()

        module.rootManager.contentEntries.forEach { contentEntry ->
            contentEntry.sourceFolders
                .filter { it.rootType is JavaSourceRootType }
                .filter { !sourceFolderService.isAutogeneratedSourceFolder(it) }
                .forEach { intellijSourceFolder ->
                    val virtualFile = intellijSourceFolder.file
                    if (virtualFile != null) {
                        val sourceFolderPath = Path(virtualFile.path)

                        if (sourceFolderPath.startsWith(buildUnitDir)) {
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