package com.cquilez.pitesthelper.extensions

import com.cquilez.pitesthelper.domain.model.CodeItem
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiFile

interface LanguageProcessor {
    fun findNavigatableModule(navigatable: Navigatable): Module

    fun findVirtualFile(navigatable: Navigatable): VirtualFile

    fun getQualifiedName(psiFile: PsiFile): String

    fun createClassCodeItem(navigatable: Navigatable, psiFile: PsiFile): CodeItem
}