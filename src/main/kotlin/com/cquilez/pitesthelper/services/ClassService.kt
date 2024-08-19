package com.cquilez.pitesthelper.services

import com.cquilez.pitesthelper.exception.PitestHelperException
import com.intellij.openapi.components.Service
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

@Service(Service.Level.PROJECT)
class ClassService {
    fun getPublicJavaClass(psiFile: PsiFile): PsiClass {
        if (psiFile is PsiJavaFile) {
            return psiFile.classes.first { it.isPhysical }
        }
        throw PitestHelperException("Invalid class")
    }

    fun getPublicKotlinClass(ktFile: KtFile): KtClass {
        return ktFile.children.filterIsInstance<KtClass>().first { it.isPhysical }
    }

    fun isCodeFile(psiFile: PsiFile): Boolean {
        return psiFile is PsiJavaFile
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