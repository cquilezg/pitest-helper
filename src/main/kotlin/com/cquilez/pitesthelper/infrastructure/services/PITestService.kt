package com.cquilez.pitesthelper.services

import com.cquilez.pitesthelper.domain.exception.PitestHelperException
import com.cquilez.pitesthelper.domain.model.*
import com.cquilez.pitesthelper.infrastructure.persistence.ProjectConfigPersistenceAdapter
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.psi.KtClass

object PITestService {

    /**
     * Build mutation coverage command data
     */
    fun buildMutationCoverageCommand(
        module: Module,
        mutationCoverage: MutationCoverage
    ): MutationCoverageData {
        val serviceProvider = module.project.service<ServiceProvider>()
        val projectConfigPersistenceAdapter = serviceProvider.getService<ProjectConfigPersistenceAdapter>(module.project)
        return MutationCoverageData(
            module,
            projectConfigPersistenceAdapter.preGoals,
            projectConfigPersistenceAdapter.postGoals,
            collectTargetCode(mutationCoverage.normalSource),
            collectTargetCode(mutationCoverage.testSource)
        )
    }

    /**
     * Collects target packages and classes in a List
     */
    private fun collectTargetCode(
        codeItemList: List<CodeItem>
    ): List<String> {
        val targetClassesList = mutableListOf<String>()
        codeItemList.forEach {
            if (it.codeItemType == CodeItemType.PACKAGE) {
                targetClassesList.add("${it.qualifiedName}.*")
            } else if (it.codeItemType == CodeItemType.CLASS) {
                targetClassesList.add(it.qualifiedName)
            }
        }
        return targetClassesList.sorted()
    }

    fun getTestClassQualifiedName(psiClass: PsiClass): String {
        val packageName = if (psiClass is KtClass) {
            psiClass.containingKtFile.packageFqName.asString()
        } else {
            PsiService.getPackageName(psiClass)
        }
        if (psiClass.name != null) {
            return buildFullClassName(packageName, TestService.getClassUnderTestName(psiClass.name as String))
        } else {
            throw PitestHelperException("The class name cannot be found")
        }
    }

    //TODO: test this method with a Kotlin class
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
