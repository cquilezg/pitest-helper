package com.cquilez.pitesthelper.ui.actions

import com.cquilez.pitesthelper.ui.IDEInstance
import com.cquilez.pitesthelper.ui.UiTestExtension
import com.cquilez.pitesthelper.ui.actions.CommonUITestsNew.MENU_OPTION_TEXT
import com.cquilez.pitesthelper.ui.actions.fastRightClickPath
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.enabled
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.ui.visible
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.ide.starter.driver.engine.BackgroundRun
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(UiTestExtension::class)
class BasicDialogUiTest {

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
                projectView {
                    projectViewTree.fastRightClickPath(PROJECT_NAME)
                }

                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be visible", visible, 3.seconds)
                menuOption.click()

                val dialog = x(xQuery { byTitle("Mutation Coverage") })
                dialog.shouldBe("Mutation Coverage dialog should be open", visible)

                dialog.x(xQuery {
                    and(
                        byClass("BrowserLink"),
                        byText("How to set up PITest Helper in your project")
                    )
                }).shouldBe("PITest Helper help link should be visible", visible, 3.seconds)

                dialog.x(xQuery {
                    and(
                        byClass("JLabel"),
                        byText("Target Classes:")
                    )
                }).shouldBe("Target Classes label should be visible", visible, 3.seconds)

                dialog.x(xQuery {
                    and(
                        byClass("JBTextField"),
                        byAccessibleName("Target Classes:")
                    )
                }).shouldBe("Target Classes field should be visible", visible, 3.seconds)
                    .shouldBe("Target Classes field should be enabled", enabled, 3.seconds)
                    .hasText("com.myproject.*")

                dialog.x(xQuery {
                    and(
                        byClass("JLabel"),
                        byText("Target Tests:")
                    )
                }).shouldBe("Target Tests label should be visible", visible, 3.seconds)

                dialog.x(xQuery {
                    and(
                        byClass("JBTextField"),
                        byAccessibleName("Target Tests:")
                    )
                }).shouldBe("Target Tests field should be visible", visible, 3.seconds)

                dialog.x(xQuery {
                    and(
                        byClass("JLabel"),
                        byText("Run Command:")
                    )
                }).shouldBe("Run command label should be visible", visible, 3.seconds)

                dialog.x(xQuery { byClass("EditorTextField") })
                    .shouldBe("Command text area should be visible", visible, 3.seconds)

                dialog.x(xQuery {
                    and(
                        byClass("JButton"),
                        byText("Run")
                    )
                }).shouldBe("Run button should be visible", visible, 3.seconds)
                    .shouldBe("Run button should be enabled", enabled)

                dialog.x(xQuery {
                    and(
                        byClass("JButton"),
                        byText("Cancel")
                    )
                }).shouldBe("Cancel button should be visible", visible, 3.seconds)
                    .shouldBe("Cancel button should be enabled", enabled)

                val cancelButton = x(xQuery { byText("Cancel") })
                cancelButton.click()
            }
        }
    }
}
