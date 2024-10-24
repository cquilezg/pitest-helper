package com.cquilez.pitesthelper.infrastructure.service

import com.intellij.openapi.components.Service
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile

@Service
class ProjectElementService {
    fun isJavaPsiFile(psiFileSystemItem: NavigatablePsiElement): Boolean {
        return psiFileSystemItem.containingFile.virtualFile.extension == "java"
    }

    fun isKotlinPsiFile(psiFileSystemItem: NavigatablePsiElement): Boolean {
        return psiFileSystemItem.containingFile.virtualFile.extension == "kt"
    }

    private fun hasExtension(psiFile: PsiFile, extension: String): Boolean = psiFile.virtualFile.extension == extension
}