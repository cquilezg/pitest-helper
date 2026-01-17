package com.cquilez.pitesthelper.infrastructure.adapter.sourcefolder

import com.cquilez.pitesthelper.application.port.out.SourceFolderPort
import com.cquilez.pitesthelper.domain.CodeClass
import com.cquilez.pitesthelper.domain.CodeElement
import com.cquilez.pitesthelper.domain.CodePackage
import com.cquilez.pitesthelper.domain.CodeType
import com.cquilez.pitesthelper.domain.SourceFolder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNameHelper
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache

class SourceFolderAdapter(private val project: Project) : SourceFolderPort {

    private val javaPsiFacade = JavaPsiFacade.getInstance(project)
    private val localFileSystem = LocalFileSystem.getInstance()

    override fun findCorrespondingPackage(
        pkg: CodePackage,
        oppositeSourceFolder: SourceFolder
    ): Pair<CodeElement?, String?> {
        val psiPackage = javaPsiFacade.findPackage(pkg.qualifiedName)
            ?: return createPackageError(oppositeSourceFolder, pkg)

        val packageDirectory = psiPackage.directories
            .firstOrNull { it.virtualFile.toNioPath().startsWith(oppositeSourceFolder.path) }
            ?: return createPackageError(oppositeSourceFolder, pkg)

        val correspondingPackage = CodePackage(
            path = packageDirectory.virtualFile.toNioPath(),
            qualifiedName = pkg.qualifiedName,
            sourceFolder = oppositeSourceFolder
        )
        return Pair(correspondingPackage, null)
    }

    private fun createPackageError(oppositeSourceFolder: SourceFolder, pkg: CodePackage): Pair<CodeElement?, String> {
        val codeTypeLabel = if (oppositeSourceFolder.codeType == CodeType.TEST) "test" else "production"
        return Pair(null, "Package ${pkg.qualifiedName} not found in $codeTypeLabel source folder")
    }

    override fun findCorrespondingClass(
        cls: CodeClass,
        oppositeSourceFolder: SourceFolder
    ): Pair<CodeElement?, String?> {
        val targetSimpleName: String
        val targetQualifiedName: String

        if (oppositeSourceFolder.codeType == CodeType.TEST) {
            targetSimpleName = "${cls.simpleName}Test"
            targetQualifiedName = "${cls.qualifiedName}Test"
        } else {
            if (cls.simpleName.endsWith("Test")) {
                targetSimpleName = cls.simpleName.removeSuffix("Test")
                targetQualifiedName = cls.qualifiedName.removeSuffix("Test")
            } else {
                return Pair(null, null)
            }
        }

        val sourceVirtualFile = localFileSystem.findFileByPath(oppositeSourceFolder.path.toString())
            ?: return createClassError(oppositeSourceFolder, targetQualifiedName)

        val module = ModuleUtilCore.findModuleForFile(sourceVirtualFile, project)
            ?: return createClassError(oppositeSourceFolder, targetQualifiedName)

        val psiClasses = PsiShortNamesCache.getInstance(project)
            .getClassesByName(targetSimpleName, GlobalSearchScope.moduleScope(module))

        if (psiClasses.isEmpty()) {
            return createClassError(oppositeSourceFolder, targetQualifiedName)
        }

        // If only one class found, use it
        val psiClass: PsiClass? = if (psiClasses.size == 1) {
            psiClasses[0]
        } else {
            // First try exact package match
            val sourcePackage = cls.qualifiedName.substringBeforeLast(".")
            var foundClass = findClassInSamePackage(psiClasses, sourcePackage)

            // If not found, try to find best candidate in related packages
            if (foundClass == null) {
                foundClass = findBestCandidateClass(psiClasses, sourcePackage)
            }
            foundClass
        }

        return if (psiClass != null) {
            val classFile = psiClass.containingFile?.virtualFile
            val actualQualifiedName = psiClass.qualifiedName ?: targetQualifiedName
            val actualSimpleName = psiClass.name ?: targetSimpleName
            if (classFile != null) {
                val correspondingClass = CodeClass(
                    path = classFile.toNioPath(),
                    qualifiedName = actualQualifiedName,
                    simpleName = actualSimpleName,
                    sourceFolder = oppositeSourceFolder
                )
                Pair(correspondingClass, null)
            } else {
                createClassError(oppositeSourceFolder, targetQualifiedName)
            }
        } else {
            createClassError(oppositeSourceFolder, targetQualifiedName)
        }
    }

    private fun findClassInSamePackage(psiClasses: Array<PsiClass>, packageName: String): PsiClass? {
        return psiClasses.firstOrNull { psiClass ->
            val classPackage = psiClass.qualifiedName?.substringBeforeLast(".") ?: ""
            classPackage == packageName
        }
    }

    private fun findBestCandidateClass(psiClasses: Array<PsiClass>, sourcePackage: String): PsiClass? {
        var foundClass: PsiClass? = null
        psiClasses.forEach { psiClass ->
            val candidatePackage = psiClass.qualifiedName?.substringBeforeLast(".") ?: ""
            if (PsiNameHelper.isSubpackageOf(sourcePackage, candidatePackage)
                || PsiNameHelper.isSubpackageOf(candidatePackage, sourcePackage)
            ) {
                if (foundClass == null) {
                    foundClass = psiClass
                } else {
                    return null
                }
            }
        }
        return foundClass
    }

    private fun createClassError(
        oppositeSourceFolder: SourceFolder,
        targetQualifiedName: String
    ): Pair<CodeElement?, String> {
        val codeTypeLabel = if (oppositeSourceFolder.codeType == CodeType.TEST) "test" else "production"
        return Pair(null, "Class $targetQualifiedName not found in $codeTypeLabel source folder")
    }
}