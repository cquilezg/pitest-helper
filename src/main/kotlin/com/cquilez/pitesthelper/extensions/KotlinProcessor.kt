package com.cquilez.pitesthelper.extensions

import com.cquilez.pitesthelper.exception.PitestHelperException
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.pom.Navigatable
import org.jetbrains.kotlin.idea.projectView.KtClassOrObjectTreeNode

class KotlinProcessor : LanguageProcessor {
    override fun findNavigatableModule(navigatable: Navigatable): Module {
        var module: Module? = null
        if (navigatable is KtClassOrObjectTreeNode) {
            module = ModuleUtilCore.findModuleForFile(navigatable.value.containingFile)
        }
        return module
            ?: throw PitestHelperException("There is/are elements not supported.")
    }
}