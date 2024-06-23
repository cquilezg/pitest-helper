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
                    waitFor(ofSeconds(10)) { button("OK").isEnabled() }
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
                retry(3) {
                    collapseAllButton.click()
                    projectViewTree.data[projectName].doubleClick()
                    findClass(this, parentNodeList, className)
                    projectViewTree.data[className].rightClick()
                }
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

    /**
     * Try an operation several times
     */
    private fun <T> retry(times: Int, delay: Long = 0, block: () -> T): T {
        var lastException: Exception? = null

        for (attempt in 1..times) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < times) {
                    Thread.sleep(delay)
                }
            }
        }

        throw lastException ?: IllegalStateException("Operation failed after $times attempts")
    }

    private fun findClass(projectToolWindow: ProjectToolWindow, parentNodeList: List<String>, className: String) = with(projectToolWindow) {
        for (node in parentNodeList) {
            if (projectViewTree.data.hasText(className)) {
                break
            }
            waitFor { projectViewTree.data.hasText(node) }
            projectViewTree.data[node].click()
            keyboard {
                key(VK_MULTIPLY)
            }
        }
    }
}