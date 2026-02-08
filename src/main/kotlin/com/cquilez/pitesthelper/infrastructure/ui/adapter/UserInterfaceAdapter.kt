package com.cquilez.pitesthelper.infrastructure.ui.adapter

import com.cquilez.pitesthelper.application.port.out.ProjectConfigPort
import com.cquilez.pitesthelper.application.port.out.UserInterfacePort
import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.infrastructure.service.buildsystem.BuildSystemService
import com.cquilez.pitesthelper.infrastructure.service.buildsystem.GradleBuildSystemService
import com.cquilez.pitesthelper.infrastructure.service.buildsystem.MavenBuildSystemService
import com.cquilez.pitesthelper.infrastructure.ui.MutationCoverageDialog
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class UserInterfaceAdapter(val project: Project) : UserInterfacePort {

    private val mavenBuildSystemService = project.service<MavenBuildSystemService>()
    private val gradleBuildSystemService = project.service<GradleBuildSystemService>()
    private val projectConfigPort = project.service<ProjectConfigPort>()

    override fun showMutationCoverageDialog(options: MutationCoverageOptions) {
        val commandBuilder: BuildSystemService = when (options.buildSystem) {
            BuildSystem.GRADLE -> gradleBuildSystemService
            else -> mavenBuildSystemService
        }

        showDialog({
            val dialog = MutationCoverageDialog(
                options,
                commandBuilder,
                projectConfigPort
            )
            dialog.show()
            if (dialog.isOK) {
                when (options.buildSystem) {
                    BuildSystem.GRADLE -> gradleBuildSystemService.runMutationCoverage(options)
                    else -> mavenBuildSystemService.runMutationCoverage(options)
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