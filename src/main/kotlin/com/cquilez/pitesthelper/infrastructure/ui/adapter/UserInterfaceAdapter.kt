package com.cquilez.pitesthelper.infrastructure.ui.adapter

import com.cquilez.pitesthelper.application.port.out.ProjectConfigPort
import com.cquilez.pitesthelper.application.port.out.UserInterfacePort
import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.infrastructure.adapter.buildsystem.AbstractBuildSystemAdapter
import com.cquilez.pitesthelper.infrastructure.service.buildsystem.GradleCommandRunnerService
import com.cquilez.pitesthelper.infrastructure.service.buildsystem.MavenCommandRunnerService
import com.cquilez.pitesthelper.infrastructure.ui.MutationCoverageDialog
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class UserInterfaceAdapter(val project: Project) : UserInterfacePort {

    private val mavenCommandRunnerService = project.service<MavenCommandRunnerService>()
    private val gradleCommandRunnerService = project.service<GradleCommandRunnerService>()
    private val projectConfigPort = project.service<ProjectConfigPort>()

    override fun showMutationCoverageDialog(options: MutationCoverageOptions) {
        val buildSystemPort = AbstractBuildSystemAdapter.forBuildSystem(options.buildSystem)
            ?: AbstractBuildSystemAdapter.forBuildSystem(BuildSystem.MAVEN)
            ?: return

        showDialog({
            val dialog = MutationCoverageDialog(
                options,
                buildSystemPort,
                projectConfigPort
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

    fun showDialog(mainAction: Runnable, orElseAction: Runnable) {
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            mainAction.run()
        } else {
            orElseAction.run()
        }
    }
}