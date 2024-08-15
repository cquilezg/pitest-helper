package com.cquilez.pitesthelper.ui.steps

import com.cquilez.pitesthelper.ui.components.Node
import com.cquilez.pitesthelper.ui.components.ProjectTree
import com.cquilez.pitesthelper.ui.pages.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.Keyboard
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import java.awt.Point
import java.awt.event.KeyEvent.*
import java.nio.file.Paths
import java.time.Duration.ofMinutes
import java.time.Duration.ofSeconds
import java.util.function.Consumer
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

    private fun runMutationCoverage(
        projectName: String,
        remoteRobot: RemoteRobot,
        expectedCommand: String,
        checkProjectLoaded: Boolean,
        consumer: Consumer<ProjectToolWindow>
    ) = with(remoteRobot) {
        idea {
            dumbAware {
                openProjectView(this)
                val buttons =
                    buttons(byXpath("//div[@class='ToolWindowHeader']//div[@class='ActionButton' and @myicon='hideToolWindow.svg']"))
                val hideButton = button(
                    byXpath(
                        "ActionButton type",
                        "//div[@class='ToolWindowHeader']//div[@class='JPanel']//div[@class='TabPanel']//div[@class='ContentComboLabel' and @text='Project']/../../../div[@class='JPanel']//div[@tooltiptext='Hide']"
                    )
                )
                for (button in buttons) {
                    if (button.locationOnScreen != hideButton.locationOnScreen) {
                        button.click()
                    }
                }
                projectToolWindow {
                    if (checkProjectLoaded) {
                        waitFor(ofMinutes(5)) { modulesJs() > 1 }
                    }

                    retry(3) {
                        collapseAllButton.click()
                        projectViewTree.data[projectName].doubleClick()
                        consumer.accept(this)
                    }
                }
                idea {
                    waitFor(ofMinutes(5)) { isDumbMode().not() }
                }
                actionMenuItem("Run Mutation Coverage...").click()
            }
        }
        mutationCoverageDialog {
            assertEquals(expectedCommand, commandTextArea.text)
            cancelButton.click()
        }
    }

    fun runMutationCoverage(
        projectName: String,
        remoteRobot: RemoteRobot,
        nodeList: List<String>,
        expectedCommand: String,
        checkProjectLoaded: Boolean
    ) = with(remoteRobot) {
        runMutationCoverage(projectName, remoteRobot, expectedCommand, checkProjectLoaded) {
            findLastNode(it, nodeList)
            retry(3, preRetryAction = { keyboard { key(VK_PAGE_DOWN) } }) {
                val projectTreeItems = it.projectViewTree.data.getAll()
                val projectTree = buildProjectTree(projectTreeItems)
                val node = findNodes(projectTree.nodes[0], nodeList)
                keyboard {
                    clickWithScroll(projectTreeItems, node!!, this)
                }
                node!!.remoteTexts[0].rightClick()
            }
        }
    }

    fun runMutationCoverage(
        projectName: String,
        remoteRobot: RemoteRobot,
        nodeSet: Set<List<String>>,
        expectedCommand: String,
        checkProjectLoaded: Boolean
    ) = with(remoteRobot) {
        val multiSelectKey = getMultiSelectKey(remoteRobot)
        runMutationCoverage(projectName, remoteRobot, expectedCommand, checkProjectLoaded) {
            for (stringList in nodeSet) {
                findLastNode(it, stringList)
            }
            lateinit var projectTree: ProjectTree
            var firstNode: Node? = null
            val projectTreeItems = it.projectViewTree.data.getAll()
            retry(3, preRetryAction = { keyboard { key(VK_PAGE_DOWN) } }) {
                projectTree = buildProjectTree(projectTreeItems)
                firstNode = findNodes(projectTree.nodes[0], nodeSet.first())
                keyboard {
                    clickWithScroll(projectTreeItems, firstNode!!, this)
                }
            }
            var lastNode = firstNode
            keyboard {
                for (stringList in nodeSet.drop(1)) {
                    retry(3, preRetryAction = { keyboard { key(VK_PAGE_DOWN) } }) {
                        val node = findNodes(projectTree.nodes[0], stringList)
                        lastNode = node
                        pressing(multiSelectKey) {
                            clickWithScroll(projectTreeItems, node!!, this)
                        }
                    }
                }

            }
            lastNode!!.remoteTexts[0].rightClick()
        }
    }

    private fun getMultiSelectKey(remoteRobot: RemoteRobot) = if (remoteRobot.isMac()) {
        VK_META
    } else {
        VK_CONTROL
    }

    private fun clickWithScroll(projectTreeItems: List<RemoteText>, node: Node, keyboard: Keyboard) = with(keyboard) {
        if (projectTreeItems.subList(projectTreeItems.size - 3, projectTreeItems.size)
                .contains(node.remoteTexts[0])
        ) {
            key(VK_PAGE_DOWN)
        }
        node.remoteTexts[0].click()
    }


    private fun findNodes(nodeTree: Node, nodeNames: List<String>): Node? {
        val parentNode = findNodeInTree(nodeTree, nodeNames[0], 10)
        var currentNode: Node? = null
        if (parentNode != null) {
            currentNode = parentNode
            for (currentNodeName in nodeNames.drop(1)) {
                currentNode = findChildNode(currentNode!!.children, currentNodeName, 0)
                if (currentNode == null) {
                    break
                }
            }
        }
        return currentNode
    }

    private fun findChildNode(childNodes: List<Node>, nodeName: String, subLevels: Int): Node? {
        for (childNode in childNodes) {
            val remoteText = childNode.remoteTexts.firstOrNull { it.text == nodeName }
            if (remoteText != null) {
                return childNode
            }
            if (subLevels > 0) {
                findChildNode(childNode.children, nodeName, subLevels - 1)
            }
        }
        return null
    }

    private fun findNodeInTree(nodeTree: Node, nodeName: String, subLevels: Int): Node? {
        val remoteText = nodeTree.remoteTexts.firstOrNull { it.text == nodeName }
        if (remoteText != null) {
            return nodeTree
        }
        if (subLevels > 0) {
            return findChildNode(nodeTree.children, nodeName, subLevels - 1)
        }
        return null
    }

    private fun buildProjectTree(elements: List<RemoteText>): ProjectTree {
        val projectTree = ProjectTree(mutableListOf())
        val root = Node(mutableListOf(elements.first()), null, mutableListOf())
        projectTree.nodes.add(root)
        var lastNode = root
        for (remoteText in elements.drop(1)) {
            lateinit var currentNode: Node
            if (remoteText.point.x > lastNode.remoteTexts.first().point.x
                && remoteText.point.y > lastNode.remoteTexts.first().point.y
            ) {
                currentNode = Node(mutableListOf(remoteText), lastNode, mutableListOf())
                lastNode.children.add(currentNode)
            } else if (remoteText.point.x == lastNode.remoteTexts.last().point.x) {
                currentNode = Node(mutableListOf(remoteText), lastNode.parent, mutableListOf())
                if (lastNode.parent != null) {
                    lastNode.parent!!.children.add(currentNode)
                } else {
                    projectTree.nodes.add(currentNode)
                }
            } else if (remoteText.point.x < lastNode.remoteTexts.first().point.x) {
                val sibling = findSiblingNode(lastNode, remoteText.point)
                currentNode = Node(mutableListOf(remoteText), sibling!!.parent, mutableListOf())
                if (sibling.parent != null) {
                    sibling.parent.children.add(currentNode)
                } else {
                    projectTree.nodes.add(currentNode)
                }
            } else {
                lastNode.remoteTexts.add(remoteText)
                currentNode = lastNode
            }
            lastNode = currentNode
        }
        return projectTree
    }

    private fun findSiblingNode(lastNode: Node, point: Point): Node? {
        var currentNode = lastNode
        while (currentNode.parent != null) {
            val parentNode = currentNode.parent!!
            if (parentNode.remoteTexts.first().point.x == point.x) {
                return parentNode
            }
            currentNode = parentNode
        }
        return null
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

    /**
     * Try an operation several times
     */
    private fun <T> retry(times: Int, delay: Long = 0, preRetryAction: () -> Unit, block: () -> T): T {
        var lastException: Exception? = null

        for (attempt in 1..times) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < times) {
                    Thread.sleep(delay)
                    preRetryAction.invoke()
                }
            }
        }

        throw lastException ?: IllegalStateException("Operation failed after $times attempts")
    }

    private fun findLastNode(projectToolWindow: ProjectToolWindow, parentNodeList: List<String>) =
        with(projectToolWindow) {
            for (node in parentNodeList) {
                if (projectViewTree.data.hasText(parentNodeList.last())) {
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