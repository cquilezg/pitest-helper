package com.cquilez.pitesthelper.extensions

import com.intellij.openapi.module.Module
import com.intellij.pom.Navigatable

interface LanguageProcessor {
    fun findNavigatableModule(navigatable: Navigatable): Module
}