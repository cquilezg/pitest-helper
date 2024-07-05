package com.cquilez.pitesthelper.model

import com.intellij.openapi.module.Module

data class MutationCoverageCommandData(
    val module: Module,
    var targetClasses: String,
    var targetTests: String
)