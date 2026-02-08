package com.cquilez.pitesthelper.ui.actions

import com.cquilez.pitesthelper.ui.IDEInstance
import com.cquilez.pitesthelper.ui.UiTestExtension
import com.cquilez.pitesthelper.ui.fixtures.mutationCoverageDialog
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.enabled
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.ui.visible
import com.intellij.ide.starter.driver.engine.BackgroundRun
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(UiTestExtension::class)
class BasicDialogUiTest : AbstractRunMutationCoverageUITest(PROJECT_NAME) {

    @IDEInstance(testName = "basicDialogUiTest", projectPath = PROJECT_NAME)
    private lateinit var run: BackgroundRun

    companion object {
        private const val PROJECT_NAME = "sample-maven"
    }

    @AfterEach
    fun afterEachHook() = CommonUITestsNew.closeMutationCoverageDialogIfOpen(run)

    @Test
    fun allMutationCoverageComponentsAreVisible() {
        run.driver.withContext {
            ideFrame {
                rightClickPath(projectView(), fullMatch = false)
                clickRunMutationCoverageMenuOption()

                mutationCoverageDialog {
                    helpLink.shouldBe("PITest Helper help link should be visible", visible, 1.seconds)
                    targetClassesLabel.shouldBe("Target Classes label should be visible", visible, 1.seconds)
                    targetClassesField
                        .shouldBe("Target Classes field should be visible", visible, 1.seconds)
                        .shouldBe("Target Classes field should be enabled", enabled, 1.seconds)
                    assertEquals("com.myproject.*", targetClassesField.text)

                    targetTestsLabel.shouldBe("Target Tests label should be visible", visible, 1.seconds)
                    targetTestsField.shouldBe("Target Tests field should be visible", visible, 1.seconds)

                    runCommandLabel.shouldBe("Run command label should be visible", visible, 1.seconds)
                    commandTextArea.shouldBe("Command text area should be visible", visible, 1.seconds)

                    runButton
                        .shouldBe("Run button should be visible", visible, 1.seconds)
                        .shouldBe("Run button should be enabled", enabled)
                    cancelButton
                        .shouldBe("Cancel button should be visible", visible, 1.seconds)
                        .shouldBe("Cancel button should be enabled", enabled)

                    cancelButton.click()
                }
            }
        }
    }
}
