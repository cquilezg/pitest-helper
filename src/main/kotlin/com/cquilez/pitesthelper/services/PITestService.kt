package com.cquilez.pitesthelper.services

import com.cquilez.pitesthelper.exception.PitestHelperException
import com.intellij.psi.PsiClass

object PITestService {
    fun getQualifiedTestClassName(psiClass: PsiClass): String {
        val packageName = PsiService.getPackageName(psiClass)
        if (psiClass.name != null) {
            return buildFullClassName(packageName, TestService.getClassUnderTestName(psiClass.name as String))
        } else {
            throw PitestHelperException("The class name cannot be found")
        }
    }

    fun extractTargetTestsByPsiClass(psiClass: PsiClass): String {
        val packageName = PsiService.getPackageName(psiClass)
        if (psiClass.name != null) {
            return "$packageName.${psiClass.name}"
        } else {
            throw PitestHelperException("The test class name cannot be found")
        }
    }

    fun buildFullClassName(packageName: String, className: String) = "$packageName.${className}"
}
