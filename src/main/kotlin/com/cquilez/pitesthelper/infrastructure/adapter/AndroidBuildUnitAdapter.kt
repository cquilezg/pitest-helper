package com.cquilez.pitesthelper.infrastructure.adapter

import com.android.tools.idea.projectsystem.SourceProviderManager
import com.android.tools.idea.projectsystem.allSourceFolders
import com.cquilez.pitesthelper.application.port.out.BuildUnitPort
import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.BuildUnit
import com.cquilez.pitesthelper.domain.model.CodeType
import com.cquilez.pitesthelper.domain.model.SourceFolder
import com.cquilez.pitesthelper.infrastructure.service.CacheService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.android.facet.AndroidFacet
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

class AndroidBuildUnitAdapter(val project: Project) : BuildUnitPort {

    private val cacheService: CacheService by lazy { project.service<CacheService>() }
    private val logger = thisLogger()

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

    private fun findSourceFoldersInModule(module: Module, buildUnit: BuildUnit): List<SourceFolder> {
        val buildUnitDir = buildUnit.buildPath.parent

        logger.info("Finding source folders for module: ${module.name}")

        val androidFacet = AndroidFacet.getInstance(module)
        if (androidFacet != null) {
            logger.info("Android facet found for module: ${module.name}, using AndroidFacet API")
            return findSourceFoldersUsingAndroidFacet(androidFacet, buildUnit, buildUnitDir)
        }

        logger.info("No Android facet for module: ${module.name}, using ModuleRootManager fallback")
        return findSourceFoldersUsingModuleRootManager(module, buildUnit, buildUnitDir)
    }

    private fun findSourceFoldersUsingAndroidFacet(
        androidFacet: AndroidFacet,
        buildUnit: BuildUnit,
        buildUnitDir: Path
    ): List<SourceFolder> {
        val sourceFolders = mutableListOf<SourceFolder>()

        try {
            val sourceProviderManager = SourceProviderManager.getInstance(androidFacet)

            val currentSourceProviders = sourceProviderManager.currentSourceProviders
            logger.info("Processing ${currentSourceProviders.size} current source providers (PRODUCTION)")

            currentSourceProviders.forEach { sourceProvider ->
                logger.info("Processing production source provider: ${sourceProvider.name}")
                sourceProvider.javaDirectories.forEach { virtualFile ->
                    addSourceFolderFromVirtualFile(
                        virtualFile,
                        buildUnitDir,
                        buildUnit,
                        CodeType.PRODUCTION,
                        sourceFolders
                    )
                }
            }

            val currentTestSourceProviders = sourceProviderManager.currentHostTestSourceProviders
            logger.info("Processing ${currentTestSourceProviders.size} current test source providers (TEST)")

            currentTestSourceProviders.forEach { sourceProvider ->
                logger.info("Processing test source provider: ${sourceProvider.value}")
                sourceProvider.value.forEach { namedIdeaSourceProvider ->
                    namedIdeaSourceProvider.allSourceFolders.forEach {
                        addSourceFolderFromVirtualFile(
                            it,
                            buildUnitDir,
                            buildUnit,
                            CodeType.TEST,
                            sourceFolders
                        )
                    }
                }
            }

            logger.info("Found ${sourceFolders.size} source folders using AndroidFacet")
        } catch (e: Exception) {
            logger.error("Error using AndroidFacet API, falling back to file system scan", e)
            addSourceFoldersFromFileSystem(buildUnitDir, buildUnit, sourceFolders)
        }

        return sourceFolders.distinct()
    }

    private fun findSourceFoldersUsingModuleRootManager(
        module: Module,
        buildUnit: BuildUnit,
        buildUnitDir: Path
    ): List<SourceFolder> {
        val sourceFolders = mutableListOf<SourceFolder>()
        val moduleRootManager = ModuleRootManager.getInstance(module)

        val sourceRoots = moduleRootManager.getSourceRoots(true)
        logger.info("ModuleRootManager found ${sourceRoots.size} source roots")

        sourceRoots.forEach { sourceRoot ->
            val sourceFolderPath = Path(sourceRoot.path)

            if (sourceFolderPath.startsWith(buildUnitDir)) {
                if (isAutogeneratedSource(sourceRoot)) {
                    return@forEach
                }

                val isTest = moduleRootManager.fileIndex.isInTestSourceContent(sourceRoot)
                val codeType = if (isTest) CodeType.TEST else CodeType.PRODUCTION

                sourceFolders.add(
                    SourceFolder(
                        path = sourceFolderPath,
                        codeType = codeType,
                        buildUnit = buildUnit
                    )
                )
            }
        }

        return sourceFolders.distinct()
    }

    private fun addSourceFolderFromVirtualFile(
        virtualFile: VirtualFile,
        buildUnitDir: Path,
        buildUnit: BuildUnit,
        codeType: CodeType,
        output: MutableList<SourceFolder>
    ) {
        if (!virtualFile.exists()) {
            logger.info("VirtualFile does not exist, skipping: ${virtualFile.path}")
            return
        }

        val sourceFolderPath = Path(virtualFile.path)
        logger.info("Processing directory: ${sourceFolderPath} (${codeType})")

        if (sourceFolderPath.startsWith(buildUnitDir)) {
            if (virtualFile.name.equals("manifests", ignoreCase = true)) {
                logger.info("Skipping manifests folder")
                return
            }

            if (sourceFolderPath.toString().contains("/generated/") ||
                sourceFolderPath.toString().contains("/build/generated/")) {
                logger.info("Skipping generated source folder")
                return
            }

            logger.info("Adding source folder: $sourceFolderPath (${codeType})")
            output.add(
                SourceFolder(
                    path = sourceFolderPath,
                    codeType = codeType,
                    buildUnit = buildUnit
                )
            )
        }
    }

    private fun addSourceFoldersFromFileSystem(
        buildUnitDir: Path,
        buildUnit: BuildUnit,
        output: MutableList<SourceFolder>
    ) {
        logger.info("Using file system fallback for: $buildUnitDir")

        val standardPaths = listOf(
            "src/main/java" to CodeType.PRODUCTION,
            "src/main/kotlin" to CodeType.PRODUCTION,
            "src/test/java" to CodeType.TEST,
            "src/test/kotlin" to CodeType.TEST,
            "src/androidTest/java" to CodeType.TEST,
            "src/androidTest/kotlin" to CodeType.TEST
        )

        standardPaths.forEach { (relativePath, codeType) ->
            val sourcePath = buildUnitDir.resolve(relativePath)
            if (java.nio.file.Files.exists(sourcePath)) {
                logger.info("File system fallback found: $sourcePath")
                output.add(
                    SourceFolder(
                        path = sourcePath,
                        codeType = codeType,
                        buildUnit = buildUnit
                    )
                )
            }
        }
    }

    private fun isAutogeneratedSource(virtualFile: VirtualFile): Boolean {
        val path = virtualFile.path
        val name = virtualFile.name

        return path.contains("/generated/") ||
               path.contains("/build/generated/") ||
               path.contains("/.gradle/") ||
               name.equals("manifests", ignoreCase = true) ||
               name.startsWith("generated")
    }
}

