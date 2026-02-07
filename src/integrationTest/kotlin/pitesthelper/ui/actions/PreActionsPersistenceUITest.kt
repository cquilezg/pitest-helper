package com.cquilez.pitesthelper.ui.actions

import com.cquilez.pitesthelper.ui.IDEInstance
import com.cquilez.pitesthelper.ui.UiTestExtension
import com.cquilez.pitesthelper.ui.fixtures.mutationCoverageDialog
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.notPresent
import com.intellij.driver.sdk.ui.should
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.ui.visible
import com.intellij.ide.starter.driver.engine.BackgroundRun
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.awt.event.KeyEvent
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(UiTestExtension::class)
class PreActionsPersistenceUITest : AbstractRunMutationCoverageUITest(PROJECT_NAME) {

    @IDEInstance(testName = "preActionsPersistenceUiTest", projectPath = PROJECT_NAME)
    private lateinit var run: BackgroundRun

    companion object {
        private const val PROJECT_NAME = "sample-maven"
        private const val PRE_ACTIONS_TEXT = "clean compile"
        private val CLASS_A_NODE = arrayOf("src", "main", "java", "com.myproject", "package1", "ClassA")
    }

    @AfterEach
    fun afterEachHook() = CommonUITestsNew.closeMutationCoverageDialogIfOpen(run)

    @Test
    fun preActionsTextIsPersistedAfterRunAndShownWhenReopeningDialog() {
        run.driver.withContext {
            ideFrame {
                rightClickPath(projectView(), *CLASS_A_NODE, fullMatch = false)
                clickRunMutationCoverageMenuOption()

                mutationCoverageDialog {
                    modifyOptions
                        .shouldBe("Modify Options link should be visible", visible, 1.seconds)
                        .click()

                    keyboard { key(KeyEvent.VK_ENTER) }

                    preGoalsField.shouldBe("Pre Goals field should be visible", visible, 1.seconds)
                    preGoalsField.text = PRE_ACTIONS_TEXT
                    commandTextArea.should("Command text area should not contain pre goals after clearing. Actual: ${commandTextArea.text}", 1.seconds) {
                        text == "mvn $PRE_ACTIONS_TEXT pitest:mutationCoverage -DtargetClasses=com.myproject.package1.ClassA -DtargetTests=com.myproject.package1.ClassATest"
                    }

                    runButton.click()
                }

                rightClickPath(projectView(), *CLASS_A_NODE, fullMatch = false)
                clickRunMutationCoverageMenuOption()

                mutationCoverageDialog {
                    assertEquals(preGoalsField.text, PRE_ACTIONS_TEXT)
                    preGoalsField.should("Pre Goals were saved", 1.seconds) {
                        text == PRE_ACTIONS_TEXT
                    }
                    preGoalsField.text = ""
                    commandTextArea.should("Command text area should not contain pre goals after clearing. Actual: ${commandTextArea.text}", 1.seconds) {
                        text == "mvn pitest:mutationCoverage -DtargetClasses=com.myproject.package1.ClassA -DtargetTests=com.myproject.package1.ClassATest"
                    }
                    runButton.click()
                }

                rightClickPath(projectView(), *CLASS_A_NODE, fullMatch = false)
                clickRunMutationCoverageMenuOption()

                mutationCoverageDialog {
                    preGoalsField.shouldBe(
                        "Pre Goals field should not be visible when reopening after clearing",
                        notPresent,
                        1.seconds
                    )
                }
            }
        }
    }
}
