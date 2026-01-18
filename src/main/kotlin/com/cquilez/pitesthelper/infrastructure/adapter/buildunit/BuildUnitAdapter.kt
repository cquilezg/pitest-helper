package com.cquilez.pitesthelper.infrastructure.adapter.buildunit

import com.cquilez.pitesthelper.application.port.out.BuildUnitPort
import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.BuildUnit
import com.cquilez.pitesthelper.domain.SourceFolder
import com.cquilez.pitesthelper.infrastructure.service.cache.CacheService
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import java.nio.file.Path

class BuildUnitAdapter(val project: Project) : BuildUnitPort {

    private val cacheService: CacheService by lazy { project.service<CacheService>() }

    override fun cleanScanBuildUnits(): List<BuildUnit> {
        cacheService.buildUnitCache.clear()
        cacheService.sourceFolderCache.clear()

        val buildSystem = detectBuildSystem() ?: return emptyList()
        val service = AbstractBuildUnitServiceAdapter.forBuildSystem(buildSystem) ?: return emptyList()

        val buildUnits = service.scanBuildUnits(project)

        buildUnits.forEach { buildUnit ->
            cacheBuildUnitRecursively(buildUnit)
        }

        return buildUnits
    }

    private fun cacheBuildUnitRecursively(buildUnit: BuildUnit) {
        cacheService.buildUnitCache[buildUnit.buildPath] = buildUnit
        buildUnit.sourceFolders.forEach { sourceFolder ->
            cacheService.sourceFolderCache[sourceFolder.path] = sourceFolder
        }
        buildUnit.buildUnits.forEach { childBuildUnit ->
            cacheBuildUnitRecursively(childBuildUnit)
        }
    }

    private fun detectBuildSystem(): BuildSystem? {
        val modules = ModuleManager.getInstance(project).modules
        for (module in modules) {
            val contentRoots = ModuleRootManager.getInstance(module).contentRoots
            for (contentRoot in contentRoots) {
                contentRoot.findChild("pom.xml")?.let { if (it.exists()) return BuildSystem.MAVEN }
                contentRoot.findChild("build.gradle.kts")?.let { if (it.exists()) return BuildSystem.GRADLE }
                contentRoot.findChild("build.gradle")?.let { if (it.exists()) return BuildSystem.GRADLE }
            }
        }
        return null
    }

    override fun getAllBuildUnits(): List<BuildUnit> = cacheService.buildUnitCache.values.toList()

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
}
