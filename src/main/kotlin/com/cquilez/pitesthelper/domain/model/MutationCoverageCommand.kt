package com.cquilez.pitesthelper.domain.model

import com.intellij.openapi.module.Module

data class MutationCoverageCommand(
    val module: Module,
    var preActions: String,
    var postActions: String,
    var targetClasses: String,
    var targetTests: String
)