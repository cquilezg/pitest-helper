package com.cquilez.pitesthelper.services

import com.cquilez.pitesthelper.domain.exception.PitestHelperException
import com.intellij.openapi.components.Service
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile

@Service(Service.Level.PROJECT)
class ClassService {
    fun getPublicJavaClass(psiFile: PsiFile): PsiClass {
        if (psiFile is PsiJavaFile) {
            return psiFile.classes.first { it.isPhysical }
        }
        throw PitestHelperException("Invalid class")
    }

    fun getPackageName(psiClass: PsiClass): String {
        val psiFile = psiClass.containingFile
        if (psiFile is PsiJavaFile) {
            return psiFile.packageName
        } else {
            throw PitestHelperException("Invalid language")
        }
    }
}