package com.cquilez.pitesthelper.model

import com.intellij.openapi.module.Module

class MutationCoverageData(
    val module: Module,
    val preActions: String,
    val postActions: String,
    val targetClasses: List<String>,
    val targetTests: List<String>
)