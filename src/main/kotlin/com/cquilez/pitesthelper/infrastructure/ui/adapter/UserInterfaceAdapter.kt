package com.cquilez.pitesthelper.infrastructure.ui.adapter

import com.cquilez.pitesthelper.application.port.out.BuildSystemPort
import com.cquilez.pitesthelper.application.port.out.UserInterfacePort
import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.infrastructure.service.GradleCommandRunnerService
import com.cquilez.pitesthelper.infrastructure.service.MavenCommandRunnerService
import com.cquilez.pitesthelper.services.UIService
import com.cquilez.pitesthelper.infrastructure.ui.MutationCoverageDialog
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class UserInterfaceAdapter(val project: Project) : UserInterfacePort {

    val uiService = project.service<UIService>()
    private val mavenCommandRunnerService = project.service<MavenCommandRunnerService>()
    private val gradleCommandRunnerService = project.service<GradleCommandRunnerService>()

    override fun showMutationCoverageDialog(options: MutationCoverageOptions) {
        val buildSystemPort = BuildSystemPort.forBuildSystem(options.buildSystem)
            ?: BuildSystemPort.forBuildSystem(BuildSystem.MAVEN)
            ?: return

        uiService.showDialog({
            val dialog = MutationCoverageDialog(
                options,
                buildSystemPort
            )
            dialog.show()
            if (dialog.isOK) {
                when (options.buildSystem) {
                    BuildSystem.GRADLE -> gradleCommandRunnerService.runMutationCoverage(options)
                    else -> mavenCommandRunnerService.runMutationCoverage(options)
                }
            }
        }, orElseAction = {})
    }
}