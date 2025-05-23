package com.cquilez.pitesthelper.services

import com.cquilez.pitesthelper.MyBundle
import com.cquilez.pitesthelper.exception.PitestHelperException
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.util.containers.stream
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JavaSourceRootType
import java.util.function.Predicate


@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {

    private val helpMessage =
        "Please select only Java classes, packages or a module folder containing Java source code."

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }

    fun getMainSourceFolders(modules: List<Module>): List<SourceFolder> =
        modules.flatMap { module ->
            getFilteredSourceFolders(module) {
                isMainSourceFolder(it)
            }
        }

    private fun getTestSourceFolders(module: Module) =
        getFilteredSourceFolders(module) {
            isTestSourceFolder(it)
        }

    fun isMainSourceFolder(sourceFolder: SourceFolder) =
        sourceFolder.rootType == JavaSourceRootType.SOURCE && !isAutogeneratedSourceFolder(
            sourceFolder
        )

    fun isTestSourceFolder(sourceFolder: SourceFolder) =
        sourceFolder.rootType == JavaSourceRootType.TEST_SOURCE

    fun getTestSourceFolders(modules: List<Module>) =
        modules.flatMap { module ->
            getFilteredSourceFolders(module) {
                it.rootType is JavaSourceRootType && it.rootType.isForTests
            }
        }

    fun getFilteredSourceFolders(module: Module, sourceFolderFilter: Predicate<SourceFolder>): List<SourceFolder> {
        return module.rootManager.contentEntries.stream()
            .flatMap { it.sourceFolders.stream() }
            .filter(sourceFolderFilter)
            .toList()
    }

    fun getSourceRoot(project: Project, virtualFile: VirtualFile): VirtualFile? =
        ProjectRootManager.getInstance(project).fileIndex.getSourceRootForFile(virtualFile)

    fun isTestSourceRoot(module: Module, virtualFile: VirtualFile) =
        getTestSourceFolders(module).any {
            it.file == virtualFile
        }

    fun isAutogeneratedSourceFolder(sourceFolder: SourceFolder): Boolean {
        if (sourceFolder.jpsElement.properties is JavaSourceRootProperties) {
            return (sourceFolder.jpsElement.properties as JavaSourceRootProperties).isForGeneratedSources
        }
        return false
    }

    fun findClassesInModules(className: String, project: Project, modules: List<Module>): Array<PsiClass> {
        return modules.flatMap {
            PsiShortNamesCache.getInstance(project).getClassesByName(className, GlobalSearchScope.moduleScope(it))
                .toList()
        }.toTypedArray()
    }

    fun findNavigatableModule(
        project: Project,
        languageProcessorService: LanguageProcessorService,
        navigatable: Navigatable
    ): Module {
        val module: Module? = if (navigatable is PsiDirectoryNode) {
            ModuleUtilCore.findModuleForFile(navigatable.value.virtualFile, project)
        } else {
            getFileModule(project, languageProcessorService, navigatable)
        }
        return module
            ?: throw PitestHelperException("There is/are elements not supported. $helpMessage")
    }

    private fun getFileModule(
        project: Project,
        languageProcessorService: LanguageProcessorService,
        navigatable: Navigatable
    ): Module {
        var module: Module? = null
        if (isJavaNavigatable(navigatable)) {
            module = ModuleUtilCore.findModuleForFile(findJavaVirtualFile(navigatable), project)
        } else if (isKotlinNavigatable(navigatable)) {
            val kotlinProcessor = languageProcessorService.getKotlinExtension()
            module = kotlinProcessor.findNavigatableModule(navigatable)
        }
        return module
            ?: throw PitestHelperException("There is/are elements not supported. $helpMessage")
    }

    private fun findJavaVirtualFile(navigatable: Navigatable): VirtualFile {
        if (navigatable is ClassTreeNode) {
            val virtualFile = navigatable.virtualFile
            if (virtualFile != null) {
                return virtualFile
            }
        }
        throw PitestHelperException("The Java virtual file was not found. $helpMessage")
    }

    fun isSupportedPsiFile(psiFile: PsiFile): Boolean {
        return hasExtension(psiFile, "java") || hasExtension(psiFile, "kt")
    }

    fun isJavaNavigatable(navigatable: Navigatable): Boolean {
        return navigatable is ClassTreeNode && navigatable.virtualFile?.extension == "java"
    }

    fun isKotlinNavigatable(navigatable: Navigatable): Boolean {
        return navigatable is AbstractTreeNode<*> && navigatable.value is PsiElement
                && hasExtension((navigatable.value as PsiElement).containingFile, "kt")
    }

    private fun hasExtension(psiFile: PsiFile, extension: String): Boolean = psiFile.virtualFile.extension == extension

    fun getModuleFromElement(psiElement: PsiElement): Module =
        ModuleUtil.findModuleForPsiElement(psiElement)
            ?: throw PitestHelperException("Module was not found!")
}
