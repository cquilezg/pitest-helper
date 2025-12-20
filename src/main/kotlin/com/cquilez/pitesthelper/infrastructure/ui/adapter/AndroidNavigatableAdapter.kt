package com.cquilez.pitesthelper.infrastructure.ui.adapter

import com.android.tools.idea.navigator.nodes.android.AndroidModuleNode
import com.android.tools.idea.navigator.nodes.android.AndroidSourceTypeNode
import com.intellij.pom.Navigatable
import org.jetbrains.kotlin.idea.base.projectStructure.externalProjectPath
import java.nio.file.Path
import java.nio.file.Paths

class AndroidNavigatableAdapter() : KotlinNavigatableAdapter() {
    override fun getAbsolutePath(navigatable: Navigatable): Path? {
        val kotlinPath = super.getAbsolutePath(navigatable)
        if (kotlinPath != null) {
            return kotlinPath
        }

        when (navigatable) {
            is AndroidModuleNode -> {
                val projectPath = navigatable.value.externalProjectPath
                return if (projectPath != null) Paths.get(projectPath) else null
            }
            is AndroidSourceTypeNode -> {
                val mainFolder = navigatable.folders.firstOrNull()
                val path = mainFolder?.virtualFile?.path
                return if (path != null) Paths.get(path) else null
            }
            else -> return null
        }
    }
}

