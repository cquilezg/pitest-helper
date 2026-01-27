package com.cquilez.pitesthelper.domain

import java.util.Collections.emptyList

data class MutationCoverageOptions(
    var targetClasses: String = "",
    var targetTests: String = "",
    var preActions: String = "",
    var postActions: String = "",
    var errors: List<String> = emptyList(),
    var workingUnit: BuildUnit,
    val buildSystem: BuildSystem,
    val isSubmodule: Boolean = false,
    val buildUnits: List<BuildUnit> = emptyList()
)