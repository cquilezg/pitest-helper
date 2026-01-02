package com.cquilez.pitesthelper.infrastructure.adapter

import com.cquilez.pitesthelper.application.port.out.BuildUnitPort
import com.cquilez.pitesthelper.application.port.out.CodeElementPort
import com.cquilez.pitesthelper.application.port.out.PsiPort
import com.cquilez.pitesthelper.domain.model.CodeClass
import com.cquilez.pitesthelper.domain.model.CodeElement
import com.cquilez.pitesthelper.domain.model.CodePackage
import com.cquilez.pitesthelper.domain.model.CodeType
import com.cquilez.pitesthelper.domain.model.SourceFolder
import com.cquilez.pitesthelper.infrastructure.service.CacheService
import com.cquilez.pitesthelper.infrastructure.service.SourceFolderService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import java.nio.file.Path
import kotlin.io.path.isDirectory

class CodeElementAdapter(project: Project) : CodeElementPort {

    private val cacheService = project.service<CacheService>()
    private val psiManager = PsiManager.getInstance(project)
    private val localFileSystem = LocalFileSystem.getInstance()
    private val psiPort = ApplicationManager.getApplication().service<PsiPort>()
    private val buildUnitPort = project.service<BuildUnitPort>()
    private val sourceFolderService = project.service<SourceFolderService>()

    override fun getCodeElements(nodes: List<Path>): Pair<List<CodeElement>, List<String>> {
        val result = mutableListOf<CodeElement>()
        val errorMessages = mutableListOf<String>()

        nodes.forEach { nodePath ->
            if (buildUnitPort.isPathBuildUnit(nodePath)) {
                handleBuildUnitPath(nodePath, errorMessages, result)
                return@forEach
            }

            if (sourceFolderService.isPathSourceFolder(nodePath)) {
                handleSourceFolderPath(nodePath, errorMessages, result)
                return@forEach
            }

            val sourceFolder = findSourceFolderForPath(nodePath)
            if (sourceFolder == null) {
                errorMessages.add("Node $nodePath: source folder not found")
                return@forEach
            }

            val psiElement = findPsiElement(nodePath)
            if (psiElement == null) {
                errorMessages.add("Node $nodePath: PSI element not found")
                return@forEach
            }

            val codeElement = psiPort.getCodeElement(psiElement, sourceFolder)
            if (codeElement != null) {
                result.add(codeElement)
            } else {
                errorMessages.add("Unrecognized node $nodePath: not a Kotlin or Java file or package")
            }
        }

        return Pair(result, errorMessages)
    }

    private fun handleBuildUnitPath(
        nodePath: Path,
        errorMessages: MutableList<String>,
        result: MutableList<CodeElement>
    ) {
        val buildUnit = cacheService.getBuildUnitByDirectory(nodePath)
        if (buildUnit == null) {
            errorMessages.add("Node $nodePath: BuildUnit not found in cache")
            return
        }

        val productionSourceFolder = buildUnit.sourceFolders
            .firstOrNull { it.codeType == CodeType.PRODUCTION }

        if (productionSourceFolder == null) {
            errorMessages.add("Node $nodePath: No production source folder found")
            return
        }

        val virtualFile = localFileSystem.findFileByPath(productionSourceFolder.path.toString())
        if (virtualFile == null) {
            errorMessages.add("Node $nodePath: Source folder virtual file not found")
            return
        }

        val basePackage = getBasePackage(virtualFile)
        if (basePackage.isEmpty()) {
            errorMessages.add("Node $nodePath: Unable to determine base package")
            return
        }

        val codePackage = CodePackage(
            path = productionSourceFolder.path,
            qualifiedName = basePackage,
            sourceFolder = productionSourceFolder
        )
        result.add(codePackage)
    }

    private fun handleSourceFolderPath(
        nodePath: Path,
        errorMessages: MutableList<String>,
        result: MutableList<CodeElement>
    ) {
        val sourceFolder = cacheService.getSourceFolder(nodePath)
        if (sourceFolder == null) {
            errorMessages.add("Node $nodePath: SourceFolder not found in cache")
            return
        }

        val virtualFile = localFileSystem.findFileByPath(sourceFolder.path.toString())
        if (virtualFile == null) {
            errorMessages.add("Node $nodePath: Source folder virtual file not found")
            return
        }

        val basePackage = getBasePackage(virtualFile)
        if (basePackage.isEmpty()) {
            errorMessages.add("Node $nodePath: Unable to determine base package")
            return
        }

        val codePackage = CodePackage(
            path = sourceFolder.path,
            qualifiedName = basePackage,
            sourceFolder = sourceFolder
        )
        result.add(codePackage)
    }

    private fun findPsiElement(path: Path): PsiFileSystemItem? {
        var psi = cacheService.getPsiElement(path)
        if (psi?.isValid == true) {
            return psi
        }

        val virtualFile = localFileSystem.findFileByPath(path.toString()) ?: return null

        psi = if (path.isDirectory()) {
            psiManager.findDirectory(virtualFile)
        } else {
            psiManager.findFile(virtualFile)
        }

        if (psi != null) {
            cacheService.savePsiElement(path, psi)
        }

        return psi
    }

    private fun findSourceFolderForPath(path: Path): SourceFolder? {
        return cacheService.sourceFolderCache.values.find { sourceFolder ->
            path.startsWith(sourceFolder.path)
        }
    }

    private fun getBasePackage(rootFile: VirtualFile): String {
        var lastNode = rootFile
        while (lastNode.children.size == 1 && lastNode.children[0].isDirectory) {
            lastNode = lastNode.children[0]
        }
        val relativePath = VfsUtil.getRelativePath(lastNode, rootFile)
        return relativePath?.replace("/", ".") ?: ""
    }

    override fun removeNestedElements(codeElements: List<CodeElement>): List<CodeElement> {
        val packages = codeElements.filterIsInstance<CodePackage>()
        val classes = codeElements.filterIsInstance<CodeClass>()

        val optimizedPackages = packages.filter { pkg ->
            packages.none { other ->
                other != pkg && pkg.qualifiedName.startsWith("${other.qualifiedName}.")
            }
        }

        val optimizedClasses = classes.filter { cls ->
            optimizedPackages.none { pkg ->
                cls.qualifiedName.startsWith("${pkg.qualifiedName}.")
            }
        }

        return optimizedPackages + optimizedClasses
    }
}

