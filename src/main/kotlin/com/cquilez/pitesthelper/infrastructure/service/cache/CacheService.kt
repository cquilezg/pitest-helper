package com.cquilez.pitesthelper.infrastructure.service.cache

import com.cquilez.pitesthelper.domain.BuildUnit
import com.cquilez.pitesthelper.domain.SourceFolder
import com.intellij.openapi.components.Service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class CacheService {
    private val psiCache: MutableMap<Path, PsiFileSystemItem> = mutableMapOf()
    private val virtualFileCache: MutableMap<Path, VirtualFile> = mutableMapOf()
    val buildUnitCache: MutableMap<Path, BuildUnit> = mutableMapOf()
    val sourceFolderCache: MutableMap<Path, SourceFolder> = mutableMapOf()

    fun getPsiElement(key: Path): PsiFileSystemItem? {
        return psiCache[key]
    }

    fun savePsiElement(key: Path, element: PsiFileSystemItem) {
        psiCache[key] = element
    }

    fun getBuildUnit(key: Path): BuildUnit? {
        return buildUnitCache[key]
    }

    fun saveBuildUnit(key: Path, buildUnit: BuildUnit) {
        buildUnitCache[key] = buildUnit
    }

    fun getSourceFolder(key: Path): SourceFolder? {
        return sourceFolderCache[key]
    }

    fun getBuildUnitByDirectory(directoryPath: Path): BuildUnit? {
        return buildUnitCache.values.firstOrNull { buildUnit ->
            buildUnit.buildPath.parent == directoryPath
        }
    }
}