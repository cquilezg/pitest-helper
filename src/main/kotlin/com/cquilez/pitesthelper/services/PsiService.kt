package com.cquilez.pitesthelper.services

import com.cquilez.pitesthelper.exception.PitestHelperException
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile

object PsiService {
    fun getPackageName(psiClass: PsiClass): String {
        val psiFile = psiClass.containingFile
        if (psiFile is PsiJavaFile) {
            return psiFile.packageName
        }
        throw PitestHelperException("The package name class cannot be found")
    }
}