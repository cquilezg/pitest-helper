package com.cquilez.pitesthelper.infrastructure.ui.adapter

import com.cquilez.pitesthelper.application.port.out.BuildUnitPort
import com.cquilez.pitesthelper.application.port.out.UserInterfacePort
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.infrastructure.ui.MutationCoverageDialog
import com.cquilez.pitesthelper.services.UIService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class UserInterfaceAdapter(val project: Project) : UserInterfacePort {

    val uiService = project.service<UIService>()

    override fun showMutationCoverageDialog(options: MutationCoverageOptions) {
        val buildUnitPort = project.service<BuildUnitPort>()
        val buildUnits = buildUnitPort.cleanScanBuildUnits()

        val buildSystem = options.workingUnit?.buildSystem
            ?: buildUnits.firstOrNull()?.buildSystem
                    ?: com.cquilez.pitesthelper.domain.BuildSystem.MAVEN

        uiService.showDialog({
            val dialog = MutationCoverageDialog(
                project,
                options,
                buildSystem,
                buildUnits
            )
            dialog.show()
            if (dialog.isOK) {
                //TODO: save settings and run command
            }
        }, orElseAction = {})
    }
}