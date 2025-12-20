package com.cquilez.pitesthelper.infrastructure.ui

import com.intellij.pom.Navigatable
import java.nio.file.Path

interface NavigatablePort {
    fun getAbsolutePaths(navigatables: Array<out Navigatable>): List<Path>
}