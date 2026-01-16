package com.cquilez.pitesthelper.infrastructure.adapter.buildunit

import com.cquilez.pitesthelper.application.port.out.BuildUnitServicePort
import com.cquilez.pitesthelper.domain.BuildSystem
import com.intellij.openapi.extensions.ExtensionPointName

abstract class AbstractBuildUnitServiceAdapter : BuildUnitServicePort {
    companion object {
        val EP_NAME: ExtensionPointName<BuildUnitServicePort> =
            ExtensionPointName.create("com.cquilez.pitesthelper.buildUnitServicePort")

        fun forBuildSystem(buildSystem: BuildSystem): BuildUnitServicePort? {
            return EP_NAME.extensionList.find { it.getBuildSystem() == buildSystem }
        }
    }
}