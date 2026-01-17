package com.cquilez.pitesthelper.infrastructure.ui.adapter

import com.intellij.pom.Navigatable
import org.jetbrains.kotlin.idea.projectView.KtClassOrObjectTreeNode
import org.jetbrains.kotlin.idea.projectView.KtFileTreeNode
import java.nio.file.Path

open class KotlinNavigatableAdapter : BaseNavigatableAdapter() {
    override fun getAbsolutePath(navigatable: Navigatable): Path? {
        val basePath = super.getAbsolutePath(navigatable)
        if (basePath != null) {
            return basePath
        }

        when (navigatable) {
            is KtClassOrObjectTreeNode -> {
                val virtualFile = navigatable.value.containingFile.virtualFile
                val path = virtualFile?.path
                return if (path != null) Path.of(path) else null
            }
            is KtFileTreeNode -> {
                val virtualFile = navigatable.value.virtualFile
                val path = virtualFile?.path
                return if (path != null) Path.of(path) else null
            }
            else -> return null
        }
    }
}

