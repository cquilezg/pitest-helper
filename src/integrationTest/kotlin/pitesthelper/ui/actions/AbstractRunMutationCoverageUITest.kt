package com.cquilez.pitesthelper.ui.actions

import com.cquilez.pitesthelper.ui.actions.CommonUITestsNew.MENU_OPTION_TEXT
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.toolwindows.ProjectViewToolWindowUi
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.ui.visible
import com.intellij.driver.sdk.ui.xQuery
import kotlin.time.Duration.Companion.seconds

abstract class AbstractRunMutationCoverageUITest(val projectName: String) {

    protected fun rightClickPath(projectView: ProjectViewToolWindowUi, vararg path: String, fullMatch: Boolean = true) =
        with(projectView) {
            projectViewTree.fastRightClickPath(
                projectName, *path, fullMatch = fullMatch
            )
        }

    protected fun IdeaFrameUI.clickRunMutationCoverageMenuOption() {
        val menuOptionAgain = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
        menuOptionAgain.shouldBe("Menu option should be visible", visible, 1.seconds)
        menuOptionAgain.click()
    }
}