package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.intellij.openapi.extensions.ExtensionPointName

interface BuildSystemPort {
    companion object {
        val EP_NAME: ExtensionPointName<BuildSystemPort> =
            ExtensionPointName.create("com.cquilez.pitesthelper.buildSystemPort")

        fun forBuildSystem(buildSystem: BuildSystem): BuildSystemPort? {
            return EP_NAME.extensionList.find { it.getBuildSystem() == buildSystem }
        }
    }

    fun getBuildSystem(): BuildSystem
    fun buildCommand(mutationCoverageOptions: MutationCoverageOptions): String
}