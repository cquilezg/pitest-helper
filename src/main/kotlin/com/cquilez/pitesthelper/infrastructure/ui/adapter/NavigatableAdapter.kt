package com.cquilez.pitesthelper.infrastructure.ui.adapter

import com.intellij.pom.Navigatable
import java.nio.file.Path

class NavigatableAdapter : BaseNavigatableAdapter() {
    override fun getAbsolutePaths(navigatables: Array<out Navigatable>): List<Path> {
        val absolutePaths = navigatables.mapNotNull { navigatable ->
            return@mapNotNull getAbsolutePath(navigatable)
        }
        return absolutePaths
    }
}

