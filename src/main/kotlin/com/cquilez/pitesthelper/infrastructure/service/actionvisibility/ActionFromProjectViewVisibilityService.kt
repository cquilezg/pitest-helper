package com.cquilez.pitesthelper.infrastructure.service.actionvisibility

import com.cquilez.pitesthelper.application.port.out.BuildUnitPort
import com.cquilez.pitesthelper.domain.model.SourceFolder
import com.cquilez.pitesthelper.infrastructure.ui.NavigatablePort
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.nio.file.Path

@Service
class ActionFromProjectViewVisibilityService {

    fun isActionVisible(event: AnActionEvent): Boolean {
        val project = event.project as Project
        val navigatables = event.getData(CommonDataKeys.NAVIGATABLE_ARRAY)
        val navigatableService = ApplicationManager.getApplication().service<NavigatablePort>()
        val buildUnitPort = project.service<BuildUnitPort>()
        if (navigatables == null || navigatables.isEmpty()) {
            return false
        }
        val paths = navigatableService.getAbsolutePaths(navigatables)

        if (paths.size != navigatables.size) {
            return false
        }

        val buildUnits = buildUnitPort.cleanScanBuildUnits()

        val buildUnitDirs = buildUnits.map { it.buildPath.parent }
        if (allPathsAreModules(buildUnitPort, paths)) {
            return true
        }

        val allSourceFolders = buildUnits.flatMap { it.sourceFolders }
        return allPathsAreSourceFoldersOrAreInsideThem(allSourceFolders, paths)
                || allPathsContainsSourceFoldersAndNoBuildUnits(paths, allSourceFolders, buildUnitDirs)
    }

    private fun allPathsContainsSourceFoldersAndNoBuildUnits(
        paths: List<Path>,
        allSourceFolders: List<SourceFolder>,
        buildUnitDirs: List<Path>
    ): Boolean {
        val allContainSourceFolders = paths.isNotEmpty() && paths.all { path ->
            val containsSourceFolder = allSourceFolders.any { sourceFolder ->
                sourceFolder.path.startsWith(path)
            }
            val isNotBuildUnit = !buildUnitDirs.any { it == path }
            containsSourceFolder && isNotBuildUnit
        }
        return allContainSourceFolders
    }

    private fun allPathsAreSourceFoldersOrAreInsideThem(
        allSourceFolders: List<SourceFolder>,
        paths: List<Path>
    ): Boolean {
        return paths.isNotEmpty() && paths.all { path ->
            allSourceFolders.any { sourceFolder ->
                path == sourceFolder.path || path.startsWith(sourceFolder.path)
            }
        }
    }

    private fun allPathsAreModules(
        buildUnitPort: BuildUnitPort,
        paths: List<Path>
    ) = paths.all(buildUnitPort::isPathBuildUnit)
}
