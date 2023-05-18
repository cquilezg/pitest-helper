package com.cquilez.pitesthelper.services

import com.intellij.psi.*

object ClassService {
    fun getPublicClass(psiFile: PsiFile): PsiClass {
        if (psiFile is PsiJavaFile) {
            val psiClasses: Array<PsiClass> = psiFile.classes
            return psiClasses.first { service -> service.isPhysical }
        }
        throw IllegalArgumentException("Invalid class")
    }

    fun isCodeFile(psiFile: PsiFile): Boolean {
        return psiFile is PsiJavaFile
    }
}