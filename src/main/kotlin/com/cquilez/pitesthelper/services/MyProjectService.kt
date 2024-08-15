package com.cquilez.pitesthelper.services

import com.cquilez.pitesthelper.MyBundle
import com.cquilez.pitesthelper.exception.PitestHelperException
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
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
                it.rootType is JavaSourceRootType && !it.rootType.isForTests && !isAutogeneratedSourceFolder(it)
            }
        }


    private fun getTestSourceFolders(module: Module) =
        getFilteredSourceFolders(module) {
            it.rootType is JavaSourceRootType && it.rootType.isForTests
        }

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

    fun findModuleByName(project: Project, moduleName: String): Module {
        return ModuleManager.getInstance(project).modules
            .first { module: Module -> module.name == moduleName }
    }

    fun getModuleForNavigatable(project: Project, navigatable: Navigatable): Module {
        val module = when (navigatable) {
            is ClassTreeNode -> {
                ModuleUtilCore.findModuleForFile((navigatable).virtualFile!!, project)
            }

            is PsiDirectoryNode -> {
                ModuleUtilCore.findModuleForFile((navigatable.value).virtualFile, project)
            }

            else -> {
                null
            }
        }
            ?: throw PitestHelperException("There is/are elements not supported. $helpMessage")
        return module
    }

    fun getModuleFromElement(psiElement: PsiElement): Module =
        ModuleUtil.findModuleForPsiElement(psiElement)
            ?: throw PitestHelperException("Module was not found!")
}
