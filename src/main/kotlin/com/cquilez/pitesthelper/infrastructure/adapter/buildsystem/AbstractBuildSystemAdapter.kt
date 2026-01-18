package com.cquilez.pitesthelper.infrastructure.adapter.buildsystem

import com.cquilez.pitesthelper.application.port.out.BuildSystemPort
import com.cquilez.pitesthelper.domain.BuildSystem
import com.intellij.openapi.extensions.ExtensionPointName

abstract class AbstractBuildSystemAdapter : BuildSystemPort {
    companion object {
        val EP_NAME: ExtensionPointName<BuildSystemPort> =
            ExtensionPointName.create("com.cquilez.pitesthelper.buildSystemPort")

        fun forBuildSystem(buildSystem: BuildSystem): BuildSystemPort? {
            return EP_NAME.extensionList.find { it.getBuildSystem() == buildSystem }
        }
    }
}