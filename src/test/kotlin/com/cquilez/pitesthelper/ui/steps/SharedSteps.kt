package com.cquilez.pitesthelper.ui.steps

import com.cquilez.pitesthelper.ui.pages.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import java.awt.event.KeyEvent.*
import java.nio.file.Paths
import java.time.Duration.ofMinutes
import java.time.Duration.ofSeconds
import kotlin.test.assertEquals

object SharedSteps {

    private fun openProjectView(idea: IdeaFrame) = with(idea) {
        step("Open Project View if it is closed") {
            if (projectVerticalBar.isEmpty()) {
                projectToolWindow.click()
            }
        }
    }

    fun openProjectBeforeTests(remoteRobot: RemoteRobot, projectName: String) = with(remoteRobot) {
        welcomeFrame {
            step("Open project: $projectName") {
                openProject()

                dialog("Open File or Project") {
                    val currentPath = Paths.get("").toAbsolutePath().toString()
                    pathTextField.text = "$currentPath/src/test/testData/$projectName"
                    waitFor(ofSeconds(10)) { button("OK").isEnabled() }
                    button("OK").click()
                }
            }
        }
    }

    fun closeProjectAfterTests(remoteRobot: RemoteRobot) = with(remoteRobot) {
        idea {
            if (remoteRobot.isMac()) {
                keyboard {
                    hotKey(VK_SHIFT, VK_META, VK_A)
                    enterText("Close Project")
                    enter()
                }
            } else {
                menuBar.select("File", "Close Project")
            }
        }
    }

    fun runMutationCoverage(projectName: String, remoteRobot: RemoteRobot, parentNodeList: List<String>, className: String, expectedCommand: String, checkProjectLoaded: Boolean) = with(remoteRobot) {
        idea {
            waitFor(ofMinutes(5)) { isDumbMode().not() }
            openProjectView(this)
            projectToolWindow {
                if (checkProjectLoaded) {
                    waitFor(ofMinutes(5)) { modulesJs() > 1 }
                }
                collapseAllButton.click()
                findText(projectName).doubleClick()
                findClass(this, parentNodeList, className)
                findText(className).rightClick()
            }
            idea {
                waitFor(ofMinutes(5)) { isDumbMode().not() }
            }
            actionMenuItem("Run Mutation Coverage...").click()
        }
        mutationCoverageDialog {
            assertEquals(expectedCommand, commandTextArea.text)
            cancelButton.click()
        }
    }

    private fun findClass(projectToolWindow: ProjectToolWindow, parentNodeList: List<String>, className: String) = with(projectToolWindow) {
        for (node in parentNodeList) {
            if (findAllText(className).isNotEmpty()) {
                break
            }
            findText(node).click()
            keyboard {
                key(VK_MULTIPLY)
            }
        }
    }
}