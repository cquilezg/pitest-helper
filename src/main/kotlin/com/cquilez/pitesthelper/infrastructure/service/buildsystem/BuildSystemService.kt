package com.cquilez.pitesthelper.infrastructure.service.buildsystem

import com.cquilez.pitesthelper.domain.MutationCoverageOptions

interface BuildSystemService {
    fun buildCommand(mutationCoverageOptions: MutationCoverageOptions): String

    fun normalizeAndResolveActions(actions: String, options: MutationCoverageOptions): String {
        if (actions.isEmpty()) return ""
        val normalized = actions.replace(Regex("\\s+"), " ").trim()
        val workingUnit = options.workingUnit
        if (options.buildUnits.isNotEmpty()) {
            val firstBuildUnit = options.buildUnits[0]
            if (workingUnit != firstBuildUnit && firstBuildUnit.buildUnits.size > 1) {
                val moduleName = workingUnit.name
                return normalized.split(" ").joinToString(" ") { goal ->
                    if (goal.startsWith(":")) goal else ":$moduleName:$goal"
                }
            }
        }
        return normalized
    }
}
