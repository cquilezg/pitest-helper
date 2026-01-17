package com.cquilez.pitesthelper.infrastructure.ui.adapter

import com.cquilez.pitesthelper.infrastructure.ui.NavigatablePort
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.pom.Navigatable
import java.nio.file.Path

abstract class BaseNavigatableAdapter : NavigatablePort {
    override fun getAbsolutePaths(navigatables: Array<out Navigatable>): List<Path> {
        val absolutePaths = navigatables.mapNotNull { navigatable ->
            return@mapNotNull getAbsolutePath(navigatable)
        }
        return absolutePaths
    }

    open fun getAbsolutePath(navigatable: Navigatable): Path? {
        if (navigatable is PsiDirectoryNode || navigatable is ClassTreeNode) {
            val path = navigatable.virtualFile?.path
            return if (path != null) Path.of(path) else null
        }
        return null
    }
}

