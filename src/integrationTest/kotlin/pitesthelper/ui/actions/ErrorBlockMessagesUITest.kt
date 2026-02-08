package com.cquilez.pitesthelper.ui.actions

import com.cquilez.pitesthelper.ui.IDEInstance
import com.cquilez.pitesthelper.ui.UiTestExtension
import com.cquilez.pitesthelper.ui.fixtures.mutationCoverageDialog
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.ui.visible
import com.intellij.ide.starter.driver.engine.BackgroundRun
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(UiTestExtension::class)
class ErrorBlockMessagesUITest : AbstractRunMutationCoverageUITest(PROJECT_NAME) {

    @IDEInstance(testName = "errorBlockMessagesUiTest", projectPath = PROJECT_NAME)
    private lateinit var run: BackgroundRun

    companion object {
        private const val PROJECT_NAME = "sample-maven"
        private const val ERROR_HEADER_TEXT = "Please, check the following errors:"
        private const val EXPECTED_PACKAGE_NOT_FOUND_MESSAGE = "- Package com.myproject.package3.impl not found in test source folder."
        private val PACKAGE3_IMPL_NODE = arrayOf("src", "main", "java", "com.myproject", "package3.impl")
    }

    @AfterEach
    fun afterEachHook() = CommonUITestsNew.closeMutationCoverageDialogIfOpen(run)

    @Test
    fun errorBlockShowsHeaderAndPackageNotFoundMessageWhenDialogOpenedWithErrors() {
        run.driver.withContext {
            ideFrame {
                rightClickPath(projectView(), *PACKAGE3_IMPL_NODE, fullMatch = false)
                clickRunMutationCoverageMenuOption()

                mutationCoverageDialog {
                    shouldBe("Mutation Coverage dialog should be open", visible, 1.seconds)
                    val section = errorsSection(ERROR_HEADER_TEXT)
                    val sectionTexts = section.getAllTexts().map { it.toString() }
                    assertTrue(sectionTexts.any { it == ERROR_HEADER_TEXT }) {
                        "Errors section should contain: \"$ERROR_HEADER_TEXT\""
                    }
                    assertTrue(sectionTexts.any { it == EXPECTED_PACKAGE_NOT_FOUND_MESSAGE }) {
                        "Errors section should contain: \"$EXPECTED_PACKAGE_NOT_FOUND_MESSAGE\""
                    }
                }
            }
        }
    }
}
