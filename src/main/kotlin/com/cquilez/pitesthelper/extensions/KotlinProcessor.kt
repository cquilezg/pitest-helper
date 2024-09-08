package com.cquilez.pitesthelper.extensions

import com.cquilez.pitesthelper.exception.PitestHelperException
import com.cquilez.pitesthelper.model.CodeItem
import com.cquilez.pitesthelper.model.CodeItemType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.projectView.KtClassOrObjectTreeNode
import org.jetbrains.kotlin.idea.projectView.KtFileTreeNode
import org.jetbrains.kotlin.idea.refactoring.memberInfo.qualifiedClassNameForRendering
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration

class KotlinProcessor : LanguageProcessor {
    override fun findNavigatableModule(navigatable: Navigatable): Module {
        var module: Module? = null
        if (navigatable is KtClassOrObjectTreeNode) {
            module = ModuleUtilCore.findModuleForFile(navigatable.value.containingFile)
        } else if (navigatable is KtFileTreeNode) {
            throw PitestHelperException(
                "The file ${navigatable.name} does not contain classes or objects. " +
                        "PITest only supports Kotlin classes and objects."
            )
        }
        return module
            ?: throw PitestHelperException("There is/are elements not supported.")
    }

    override fun findVirtualFile(navigatable: Navigatable): VirtualFile {
        if (navigatable is KtClassOrObjectTreeNode) {
            return navigatable.value.containingFile.virtualFile
        }
        throw PitestHelperException("There is/are elements not supported.")
    }

    override fun getQualifiedName(psiFile: PsiFile): String {
        if (psiFile is KtFile) {
            return getPublicClass(psiFile).qualifiedClassNameForRendering()
        }
        throw PitestHelperException("There is/are elements not supported.")
    }

    override fun createClassCodeItem(navigatable: Navigatable, psiFile: PsiFile): CodeItem {
        if (psiFile is KtFile) {
            val psiClass = getPublicClass(psiFile)
            return CodeItem(psiClass.name!!, psiClass.qualifiedClassNameForRendering(), CodeItemType.CLASS, navigatable)
        }
        throw PitestHelperException("There is/are elements not supported.")
    }

    private fun getPublicClass(ktFile: KtFile): KtClassOrObject {
        return ktFile.children.filter { it is KtClass || it is KtObjectDeclaration }
            .first { it.isPhysical } as KtClassOrObject
    }
}