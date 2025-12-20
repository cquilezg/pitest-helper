package com.cquilez.pitesthelper.infrastructure.adapter

import com.cquilez.pitesthelper.application.port.out.BuildUnitPort
import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.BuildUnit
import com.cquilez.pitesthelper.domain.model.CodeType
import com.cquilez.pitesthelper.domain.model.SourceFolder
import com.cquilez.pitesthelper.infrastructure.service.SourceFolderService
import com.cquilez.pitesthelper.infrastructure.service.CacheService
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jps.model.java.JavaSourceRootType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class BuildUnitAdapter(val project: Project) : BuildUnitPort {

    private val cacheService: CacheService by lazy { project.service<CacheService>() }
    private val sourceFolderService = project.service<SourceFolderService>()

    override fun cleanScanBuildUnits(): List<BuildUnit> {
        cacheService.buildUnitCache.clear()
        cacheService.sourceFolderCache.clear()

        val modules = ModuleManager.getInstance(project).modules
        val buildUnitsWithoutHierarchy = mutableListOf<BuildUnit>()

        for (module in modules) {
            val contentRoots = ModuleRootManager.getInstance(module).contentRoots

            for (contentRoot in contentRoots) {
                val buildUnit = detectBuildUnitInModule(contentRoot) ?: continue
                val sourceFolders = findSourceFoldersInModule(module, buildUnit)

                val completeBuildUnit = BuildUnit(
                    buildSystem = buildUnit.buildSystem,
                    buildPath = buildUnit.buildPath,
                    buildFileName = buildUnit.buildFileName,
                    sourceFolders = sourceFolders
                )

                cacheService.buildUnitCache[buildUnit.buildPath] = completeBuildUnit

                sourceFolders.forEach { sourceFolder ->
                    cacheService.sourceFolderCache[sourceFolder.path] = sourceFolder
                }

                buildUnitsWithoutHierarchy.add(completeBuildUnit)
            }
        }

        val buildUnitsWithHierarchy = buildHierarchy(buildUnitsWithoutHierarchy)

        buildUnitsWithHierarchy.forEach { buildUnit ->
            cacheService.buildUnitCache[buildUnit.buildPath] = buildUnit
        }

        return buildUnitsWithHierarchy
    }

    private fun buildHierarchy(buildUnits: List<BuildUnit>): List<BuildUnit> {
        if (buildUnits.isEmpty()) return buildUnits

        val sortedByDepth = buildUnits.sortedByDescending { buildUnit ->
            buildUnit.buildPath.parent.nameCount
        }

        val childrenMap = mutableMapOf<Path, MutableList<BuildUnit>>()
        val parentMap = mutableMapOf<Path, BuildUnit>()

        for (child in sortedByDepth) {
            val childDir = child.buildPath.parent
            var parent: BuildUnit? = null

            for (potentialParent in sortedByDepth) {
                if (potentialParent == child) continue
                val parentDir = potentialParent.buildPath.parent

                if (childDir.startsWith(parentDir) && childDir != parentDir) {
                    if (parent == null || parent.buildPath.parent.nameCount < parentDir.nameCount) {
                        parent = potentialParent
                    }
                }
            }

            if (parent != null) {
                childrenMap.getOrPut(parent.buildPath) { mutableListOf() }.add(child)
                parentMap[child.buildPath] = parent
            }
        }

        val buildUnitMap = mutableMapOf<Path, BuildUnit>()
        sortedByDepth.reversed().forEach { buildUnit ->
            val parent = parentMap[buildUnit.buildPath]
            val newBuildUnit = BuildUnit(
                buildSystem = buildUnit.buildSystem,
                buildPath = buildUnit.buildPath,
                buildFileName = buildUnit.buildFileName,
                sourceFolders = emptyList(), // Will be updated in second pass
                parent = parent?.let { buildUnitMap[it.buildPath] },
                children = emptyList() // Will be updated in second pass
            )
            buildUnitMap[buildUnit.buildPath] = newBuildUnit
        }

        return sortedByDepth.map { originalBuildUnit ->
            val newBuildUnit = buildUnitMap[originalBuildUnit.buildPath]!!
            val children = childrenMap[originalBuildUnit.buildPath]?.map {
                buildUnitMap[it.buildPath]!!
            } ?: emptyList()

            val updatedSourceFolders = originalBuildUnit.sourceFolders.map { sourceFolder ->
                SourceFolder(
                    path = sourceFolder.path,
                    codeType = sourceFolder.codeType,
                    buildUnit = newBuildUnit
                )
            }

            BuildUnit(
                buildSystem = newBuildUnit.buildSystem,
                buildPath = newBuildUnit.buildPath,
                buildFileName = newBuildUnit.buildFileName,
                sourceFolders = updatedSourceFolders,
                parent = newBuildUnit.parent,
                children = children
            )
        }
    }

    override fun isPathBuildUnit(path: Path): Boolean {
        return cacheService.getBuildUnitByDirectory(path) != null
    }

    private fun detectBuildUnitInModule(contentRoot: VirtualFile): BuildUnit? {
        val pomFile = contentRoot.findChild("pom.xml")
        if (pomFile != null && pomFile.exists()) {
            val pomPath = Path(pomFile.path)
            return BuildUnit(BuildSystem.MAVEN, pomPath, "pom.xml")
        }

        val gradleKtsFile = contentRoot.findChild("build.gradle.kts")
        if (gradleKtsFile != null && gradleKtsFile.exists()) {
            val gradlePath = Path(gradleKtsFile.path)
            return BuildUnit(BuildSystem.GRADLE, gradlePath, "build.gradle.kts")
        }

        val gradleFile = contentRoot.findChild("build.gradle")
        if (gradleFile != null && gradleFile.exists()) {
            val gradlePath = Path(gradleFile.path)
            return BuildUnit(BuildSystem.GRADLE, gradlePath, "build.gradle")
        }

        return null
    }

    private fun scanForBuildFiles(directory: Path, buildUnits: MutableList<BuildUnit>) {
        if (!Files.isDirectory(directory)) {
            return
        }

        val buildUnit = detectBuildUnit(directory)
        if (buildUnit != null) {
            buildUnits.add(buildUnit)
        }

        try {
            Files.list(directory).use { stream ->
                stream.filter { Files.isDirectory(it) }
                    .filter { !shouldIgnoreDirectory(it) }
                    .forEach { scanForBuildFiles(it, buildUnits) }
            }
        } catch (e: Exception) {
        }
    }

    private fun detectBuildUnit(directory: Path): BuildUnit? {
        val cachedBuildUnit = cacheService.getBuildUnit(directory)
        if (cachedBuildUnit != null) {
            return cachedBuildUnit
        }

        val pomFile = directory.resolve("pom.xml")
        if (pomFile.isRegularFile()) {
            val buildUnit = BuildUnit(BuildSystem.MAVEN, pomFile, "pom.xml")
            cacheService.saveBuildUnit(pomFile, buildUnit)
            return buildUnit
        }

        val gradleKtsFile = directory.resolve("build.gradle.kts")
        if (gradleKtsFile.isRegularFile()) {
            val buildUnit = BuildUnit(BuildSystem.GRADLE, gradleKtsFile, "build.gradle.kts")
            cacheService.saveBuildUnit(gradleKtsFile, buildUnit)
            return buildUnit
        }

        val gradleFile = directory.resolve("build.gradle")
        if (gradleFile.isRegularFile()) {
            val buildUnit = BuildUnit(BuildSystem.GRADLE, gradleFile, "build.gradle")
            cacheService.saveBuildUnit(gradleFile, buildUnit)
            return buildUnit
        }

        return null
    }

    private fun shouldIgnoreDirectory(directory: Path): Boolean {
        val dirName = directory.name
        return dirName.startsWith(".") ||
                dirName == "build" ||
                dirName == "target" ||
                dirName == "node_modules" ||
                dirName == "out" ||
                dirName == ".idea" ||
                dirName == ".gradle"
    }

    private fun findSourceFoldersInModule(module: Module, buildUnit: BuildUnit): List<SourceFolder> {
        val sourceFolders = mutableListOf<SourceFolder>()
        val buildUnitDir = buildUnit.buildPath.parent

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
                                    codeType = codeType,
                                    buildUnit = buildUnit
                                )
                            )
                        }
                    }
                }
        }

        return sourceFolders.distinct()
    }
}