package com.cquilez.pitesthelper.ui.actions

import com.cquilez.pitesthelper.ui.fixtures.mutationCoverageDialog
import com.intellij.driver.model.TreePathToRow
import com.intellij.driver.sdk.ui.components.UiComponent
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.JTreeUiComponent
import com.intellij.driver.sdk.ui.components.elements.JTreeUiComponent.PathNotFoundException
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.ui.visible
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.BackgroundRun
import org.junit.jupiter.params.provider.Arguments
import java.awt.event.KeyEvent
import java.util.stream.Stream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Fast click on a tree path using 200ms wait time instead of 1 second.
 * This bypasses the slow Kotlin-level expansion logic with 1-second waits.
 */
fun JTreeUiComponent.fastClickPath(vararg path: String, fullMatch: Boolean = true) {
    fastExpandPath(*path.sliceArray(0..path.lastIndex - 1), fullMatch = fullMatch)
    fastFindExpandedPath(*path, fullMatch = fullMatch)?.let {
        clickRow(it.row)
    } ?: throw PathNotFoundException(path.toList())
}

/**
 * Fast right-click on a tree path by using the native fixture methods.
 * This bypasses the slow Kotlin-level expansion logic with 1-second waits.
 *
 * @param projectName The expected project name (used to find the actual root node)
 * @param restOfPath The remaining path elements after the project root
 */
fun JTreeUiComponent.fastRightClickPath(vararg path: String, fullMatch: Boolean = true) {
    fastExpandPath(*path.sliceArray(0..path.lastIndex - 1), fullMatch = fullMatch)
    fastFindExpandedPath(*path, fullMatch = fullMatch)?.let {
        rightClickRow(it.row)
    } ?: throw PathNotFoundException(path.toList())
}

/**
 * Click on a tree path without expanding it first.
 * Use this when the path is already expanded (e.g., after calling fastExpandPath).
 */
fun JTreeUiComponent.fastClickPathWithoutExpand(vararg path: String, fullMatch: Boolean = true) {
    fastFindExpandedPath(*path, fullMatch = fullMatch)?.let {
        clickRow(it.row)
    } ?: throw PathNotFoundException(path.toList())
}

fun JTreeUiComponent.fastExpandPath(vararg path: String, fullMatch: Boolean = true) {
    for (subPathLength in 0 until path.size) {
        val subPath = path.sliceArray(0..subPathLength)
        fastFindExpandedPath(*subPath, fullMatch = fullMatch)?.let {
            fixture.expandRow(it.row)
            wait(50.milliseconds) // wait expand
        } ?: PathNotFoundException(path.toList())
    }
}

private fun JTreeUiComponent.fastFindExpandedPath(vararg path: String, fullMatch: Boolean): TreePathToRow? =
    fastFindExpandedPaths(*path, fullMatch = fullMatch).singleOrNull()

private fun JTreeUiComponent.fastFindExpandedPaths(
    vararg path: String,
    fullMatch: Boolean,
): List<TreePathToRow> = collectExpandedPaths().filter { expandedPath ->
    expandedPath.path.size == path.size && expandedPath.path.containsAllNodes(*path, fullMatch = fullMatch) ||
            expandedPath.path.size - 1 == path.size && expandedPath.path.drop(1)
        .containsAllNodes(*path, fullMatch = fullMatch)
}

private fun List<String>.containsAllNodes(vararg treePath: String, fullMatch: Boolean): Boolean = zip(treePath).all {
    if (fullMatch) {
        it.first.equals(it.second, true)
    } else {
        it.first.contains(it.second, true)
    }
}


private const val BASE_PACKAGE = "com.myproject"

object CommonUITestsNew {
    const val INTELLIJ_VERSION = "2025.1"
    const val MENU_OPTION_TEXT = "Run Mutation Coverage..."

    // Cache for project node names - uses identity hash code as key to avoid holding BackgroundRun references
    private val projectNodeCache = mutableMapOf<Int, String>()

    private val SOFT_WRAP_CHARS = Regex("[⤦⤥]")

    private fun stripSoftWrapChars(text: String): String = text.replace(SOFT_WRAP_CHARS, "")

    fun UiComponent.waitForTextFieldWithText(
        expectedText: String,
        description: String,
        timeout: kotlin.time.Duration
    ): UiComponent {
        val startTime = System.currentTimeMillis()
        val timeoutMs = timeout.inWholeMilliseconds
        var lastActualText = ""

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                val texts = this.getAllTexts()
                val combinedText = texts.joinToString("")
                lastActualText = combinedText
                val normalizedText = stripSoftWrapChars(combinedText)
                if (expectedText.equals(normalizedText, ignoreCase = false)) {
                    return this
                }
            } catch (_: Exception) {
                // Component might not be ready yet, continue waiting
            }
            Thread.sleep(100)
        }

        throw AssertionError(
            "Timeout(${timeout}): $description. " +
                    "Expected: $expectedText. " +
                    "Actual (normalized): ${stripSoftWrapChars(lastActualText)}"
        )
    }

    fun closeMutationCoverageDialogIfOpen(run: BackgroundRun) {
        run.driver.withContext {
            ideFrame {
                val dialogs = xx(xQuery { byTitle("Mutation Coverage") })
                if (dialogs.list().isNotEmpty()) {
                    mutationCoverageDialog { cancelButton.click() }
                }
            }
        }
    }

    /**
     * Gets the project node name (project name + path) for the given BackgroundRun.
     * The value is cached after the first call.
     */
    private fun getProjectNode(run: BackgroundRun): String {
        val key = System.identityHashCode(run)
        return projectNodeCache.getOrPut(key) {
            var projectNode = ""
            run.driver.withContext {
                ideFrame {
                    projectView {
                        val texts = getAllTexts()
                        projectNode = "${texts[0].toString().trim()} ${texts[1].toString().trim()}"
                    }
                }
            }
            println("Cached project node: $projectNode")
            projectNode
        }
    }

    /**
     * Clears the cached project node for the given BackgroundRun.
     * Call this when the IDE is closed.
     */
    fun clearProjectNodeCache(run: BackgroundRun) {
        val key = System.identityHashCode(run)
        projectNodeCache.remove(key)
    }

    fun executeSingleNodeTest(
        run: BackgroundRun,
        nodePath: Array<String>,
        expectedTargetClasses: String,
        expectedTargetTests: String,
        expectedCommand: String
    ) {
        val projectNode = getProjectNode(run)
        run.driver.withContext {
            ideFrame {
                projectView {
                    projectViewTree.fastClickPath(projectNode, *nodePath)
                    projectViewTree.fastRightClickPath(projectNode, *nodePath)
                }

                validateOptionMenuAndDialog(expectedTargetClasses, expectedTargetTests, expectedCommand)
            }
        }
    }

    fun executeMultiNodeTest(
        run: BackgroundRun,
        nodePaths: List<Array<String>>,
        expectedTargetClasses: String,
        expectedTargetTests: String,
        expectedCommand: String
    ) {
        val projectNode = getProjectNode(run)
        run.driver.withContext {
            ideFrame {
                projectView {
                    // First expand all paths
                    for (path in nodePaths) {
                        projectViewTree.fastExpandPath(projectNode, *path)
                    }

                    // Click first node (selects it)
                    projectViewTree.fastClickPathWithoutExpand(projectNode, *nodePaths[0])

                    // Click remaining nodes with Ctrl held (adds to selection)
                    keyboard {
                        pressing(KeyEvent.VK_CONTROL) {
                            for (i in 1 until nodePaths.size) {
                                projectViewTree.fastClickPathWithoutExpand(projectNode, *nodePaths[i])
                            }
                        }
                    }

                    // Right-click the last path
                    val lastPath = nodePaths.last()
                    projectViewTree.fastRightClickPath(projectNode, *lastPath)
                }

                validateOptionMenuAndDialog(expectedTargetClasses, expectedTargetTests, expectedCommand)
            }
        }
    }

    private fun IdeaFrameUI.validateOptionMenuAndDialog(
        expectedTargetClasses: String,
        expectedTargetTests: String,
        expectedCommand: String
    ) {
        val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
        menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be visible", visible, 1.seconds)
        menuOption.click()

        mutationCoverageDialog {
            targetClassesField
                .shouldBe("Target Classes field should be visible", visible, 1.seconds)
                .waitForTextFieldWithText(
                    expectedTargetClasses,
                    "Target Classes should have text: $expectedTargetClasses",
                    1.seconds
                )
            targetTestsField
                .shouldBe("Target Tests field should be visible", visible, 1.seconds)
                .waitForTextFieldWithText(
                    expectedTargetTests,
                    "Target Tests should have text: $expectedTargetTests",
                    1.seconds
                )
            commandTextArea
                .shouldBe("Command text area should be visible", visible, 1.seconds)
                .waitForTextFieldWithText(
                    expectedCommand,
                    "Command text area should have text: $expectedCommand",
                    1.seconds
                )
            cancelButton.click()
        }
    }

    // ========================================
    // Maven Test Cases (using -D prefix)
    // ========================================

    @JvmStatic
    fun singleNodeTestCases(language: String, buildCommand: String): Stream<Arguments> =
        singleNodeTestCases(language, buildCommand, "-DtargetClasses", "-DtargetTests")

    @JvmStatic
    fun multiNodeTestCases(language: String, buildCommand: String): Stream<Arguments> =
        multiNodeTestCases(language, buildCommand, "-DtargetClasses", "-DtargetTests")

    // ========================================
    // Gradle Test Cases (using -Ppitest prefix)
    // ========================================

    @JvmStatic
    fun gradleSingleNodeTestCases(language: String, buildCommand: String): Stream<Arguments> =
        singleNodeTestCases(language, buildCommand, "-Ppitest.targetClasses", "-Ppitest.targetTests")

    @JvmStatic
    fun gradleMultiNodeTestCases(language: String, buildCommand: String): Stream<Arguments> =
        multiNodeTestCases(language, buildCommand, "-Ppitest.targetClasses", "-Ppitest.targetTests")

    // ========================================
    // Gradle Multi-Module Test Cases
    // ========================================

    @JvmStatic
    fun gradleMultiModuleSingleNodeTestCases(language: String): Stream<Arguments> = Stream.of(
        // ========================================
        // Single Node Tests
        // ========================================
        Arguments.of(
            "Single Main Class with matching Test Class (lib module)",
            arrayOf("lib", "src", "main", language, BASE_PACKAGE, "package1", "ClassA"),
            "com.myproject.package1.ClassA",
            "com.myproject.package1.ClassATest",
            "gradle :lib:pitest -Ppitest.targetClasses=com.myproject.package1.ClassA -Ppitest.targetTests=com.myproject.package1.ClassATest"
        ),
        Arguments.of(
            "Single Main Package with matching Test Package (app module)",
            arrayOf("app", "src", "main", language, BASE_PACKAGE, "package2"),
            "com.myproject.package2.*",
            "com.myproject.package2.*",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package2.* -Ppitest.targetTests=com.myproject.package2.*"
        ),
        Arguments.of(
            "Single Test Class with matching Main Class (app module)",
            arrayOf("app", "src", "test", language, BASE_PACKAGE, "package2", "ClassBTest"),
            "com.myproject.package2.ClassB",
            "com.myproject.package2.ClassBTest",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package2.ClassB -Ppitest.targetTests=com.myproject.package2.ClassBTest"
        ),
        Arguments.of(
            "Single Test Package with matching Main Package (lib module)",
            arrayOf("lib", "src", "test", language, BASE_PACKAGE, "package1"),
            "com.myproject.package1.*",
            "com.myproject.package1.*",
            "gradle :lib:pitest -Ppitest.targetClasses=com.myproject.package1.* -Ppitest.targetTests=com.myproject.package1.*"
        ),

        // ========================================
        // Special Cases Tests
        // ========================================
        Arguments.of(
            "Main Class with multiple test candidates - selects test in same package (lib module)",
            arrayOf("lib", "src", "main", language, BASE_PACKAGE, "package1", "ClassC"),
            "com.myproject.package1.ClassC",
            "com.myproject.package1.ClassCTest",
            "gradle :lib:pitest -Ppitest.targetClasses=com.myproject.package1.ClassC -Ppitest.targetTests=com.myproject.package1.ClassCTest"
        ),
        Arguments.of(
            "Main Class with test in superior package (app module)",
            arrayOf("app", "src", "main", language, BASE_PACKAGE, "package3.impl", "ClassC"),
            "com.myproject.package3.impl.ClassC",
            "com.myproject.package3.ClassCTest",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package3.impl.ClassC -Ppitest.targetTests=com.myproject.package3.ClassCTest"
        )
    )

    @JvmStatic
    fun gradleMultiModuleMultiNodeTestCases(language: String): Stream<Arguments> = Stream.of(
        // ========================================
        // Multi Node Tests
        // ========================================
        Arguments.of(
            "Two Main Classes with matching Test Classes (app module)",
            listOf(
                arrayOf("app", "src", "main", language, BASE_PACKAGE, "package1", "ClassA"),
                arrayOf("app", "src", "main", language, BASE_PACKAGE, "package1", "ClassD")
            ),
            "com.myproject.package1.ClassA,com.myproject.package1.ClassD",
            "com.myproject.package1.ClassATest,com.myproject.package1.ClassDTest",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package1.ClassA,com.myproject.package1.ClassD -Ppitest.targetTests=com.myproject.package1.ClassATest,com.myproject.package1.ClassDTest"
        ),
        Arguments.of(
            "Two Test Classes with matching Main Classes (lib module)",
            listOf(
                arrayOf("lib", "src", "test", language, BASE_PACKAGE, "package1", "ClassATest"),
                arrayOf("lib", "src", "test", language, BASE_PACKAGE, "package2", "ClassBTest")
            ),
            "com.myproject.package1.ClassA,com.myproject.package2.ClassB",
            "com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest",
            "gradle :lib:pitest -Ppitest.targetClasses=com.myproject.package1.ClassA,com.myproject.package2.ClassB -Ppitest.targetTests=com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest"
        ),
        Arguments.of(
            "Two Main Packages with matching Test Packages (app module)",
            listOf(
                arrayOf("app", "src", "main", language, BASE_PACKAGE, "package1"),
                arrayOf("app", "src", "main", language, BASE_PACKAGE, "package2")
            ),
            "com.myproject.package1.*,com.myproject.package2.*",
            "com.myproject.package1.*,com.myproject.package2.*",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package1.*,com.myproject.package2.* -Ppitest.targetTests=com.myproject.package1.*,com.myproject.package2.*"
        ),
        Arguments.of(
            "Two Test Packages with matching Main Packages (app module)",
            listOf(
                arrayOf("app", "src", "test", language, BASE_PACKAGE, "package1"),
                arrayOf("app", "src", "test", language, BASE_PACKAGE, "package2")
            ),
            "com.myproject.package1.*,com.myproject.package2.*",
            "com.myproject.package1.*,com.myproject.package2.*",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package1.*,com.myproject.package2.* -Ppitest.targetTests=com.myproject.package1.*,com.myproject.package2.*"
        ),
        Arguments.of(
            "Main Class and its package - only package selected (lib module)",
            listOf(
                arrayOf("lib", "src", "main", language, BASE_PACKAGE, "package1", "ClassA"),
                arrayOf("lib", "src", "main", language, BASE_PACKAGE, "package1")
            ),
            "com.myproject.package1.*",
            "com.myproject.package1.*",
            "gradle :lib:pitest -Ppitest.targetClasses=com.myproject.package1.* -Ppitest.targetTests=com.myproject.package1.*"
        ),
        Arguments.of(
            "Test Class and parent package - only package selected (lib module)",
            listOf(
                arrayOf("lib", "src", "test", language, BASE_PACKAGE, "package2", "ClassBTest"),
                arrayOf("lib", "src", "test", language, BASE_PACKAGE, "package2")
            ),
            "com.myproject.package2.*",
            "com.myproject.package2.*",
            "gradle :lib:pitest -Ppitest.targetClasses=com.myproject.package2.* -Ppitest.targetTests=com.myproject.package2.*"
        ),

        // ========================================
        // Cross Source Tests
        // ========================================
        Arguments.of(
            "Main Class and its Test Class - both selected (app module)",
            listOf(
                arrayOf("app", "src", "main", language, BASE_PACKAGE, "package1", "ClassA"),
                arrayOf("app", "src", "test", language, BASE_PACKAGE, "package1", "ClassATest")
            ),
            "com.myproject.package1.ClassA",
            "com.myproject.package1.ClassATest",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package1.ClassA -Ppitest.targetTests=com.myproject.package1.ClassATest"
        ),
        Arguments.of(
            "Main Class and different Test Class - all classes selected (lib module)",
            listOf(
                arrayOf("lib", "src", "main", language, BASE_PACKAGE, "package1", "ClassA"),
                arrayOf("lib", "src", "test", language, BASE_PACKAGE, "package2", "ClassBTest")
            ),
            "com.myproject.package1.ClassA,com.myproject.package2.ClassB",
            "com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest",
            "gradle :lib:pitest -Ppitest.targetClasses=com.myproject.package1.ClassA,com.myproject.package2.ClassB -Ppitest.targetTests=com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest"
        )
    )

    // ========================================
    // Generic Test Case Generators
    // ========================================

    @JvmStatic
    fun singleNodeTestCases(
        language: String,
        buildCommand: String,
        targetClassesParam: String,
        targetTestsParam: String
    ): Stream<Arguments> = Stream.of(
        // ========================================
        // Single Node Tests
        // ========================================
        Arguments.of(
            "Single Main Class with matching Test Class",
            arrayOf("src", "main", language, BASE_PACKAGE, "package1", "ClassA"),
            "com.myproject.package1.ClassA",
            "com.myproject.package1.ClassATest",
            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA $targetTestsParam=com.myproject.package1.ClassATest"
        ),
        Arguments.of(
            "Single Main Package with matching Test Package",
            arrayOf("src", "main", language, BASE_PACKAGE, "package2"),
            "com.myproject.package2.*",
            "com.myproject.package2.*",
            "$buildCommand $targetClassesParam=com.myproject.package2.* $targetTestsParam=com.myproject.package2.*"
        ),
        Arguments.of(
            "Single Test Class with matching Main Class",
            arrayOf("src", "test", language, BASE_PACKAGE, "package2", "ClassBTest"),
            "com.myproject.package2.ClassB",
            "com.myproject.package2.ClassBTest",
            "$buildCommand $targetClassesParam=com.myproject.package2.ClassB $targetTestsParam=com.myproject.package2.ClassBTest"
        ),
        Arguments.of(
            "Single Test Package with matching Main Package",
            arrayOf("src", "test", language, BASE_PACKAGE, "package1"),
            "com.myproject.package1.*",
            "com.myproject.package1.*",
            "$buildCommand $targetClassesParam=com.myproject.package1.* $targetTestsParam=com.myproject.package1.*"
        ),

        // ========================================
        // Special Cases Tests
        // ========================================
        Arguments.of(
            "Main Class with multiple test candidates - selects test in same package",
            arrayOf(
                "src",
                "main",
                language,
                BASE_PACKAGE,
                "package1",
                if (language == "java") "ClassC" else "ClassC.kt"
            ),
            "com.myproject.package1.ClassC",
            "com.myproject.package1.ClassCTest",
            "$buildCommand $targetClassesParam=com.myproject.package1.ClassC $targetTestsParam=com.myproject.package1.ClassCTest"
        ),
        Arguments.of(
            "Main Class with test in superior package",
            arrayOf("src", "main", language, BASE_PACKAGE, "package3.impl", "ClassC"),
            "com.myproject.package3.impl.ClassC",
            "com.myproject.package3.ClassCTest",
            "$buildCommand $targetClassesParam=com.myproject.package3.impl.ClassC $targetTestsParam=com.myproject.package3.ClassCTest"
        )
    )

    @JvmStatic
    fun multiNodeTestCases(
        language: String,
        buildCommand: String,
        targetClassesParam: String,
        targetTestsParam: String
    ): Stream<Arguments> = Stream.of(
        // ========================================
        // Multi Node Tests
        // ========================================
        Arguments.of(
            "Two Main Classes with matching Test Classes",
            listOf(
                arrayOf("src", "main", language, BASE_PACKAGE, "package1", "ClassA"),
                arrayOf("src", "main", language, BASE_PACKAGE, "package1", "ClassD")
            ),
            "com.myproject.package1.ClassA,com.myproject.package1.ClassD",
            "com.myproject.package1.ClassATest,com.myproject.package1.ClassDTest",
            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA,com.myproject.package1.ClassD $targetTestsParam=com.myproject.package1.ClassATest,com.myproject.package1.ClassDTest"
        ),
        Arguments.of(
            "Two Test Classes with matching Main Classes",
            listOf(
                arrayOf("src", "test", language, BASE_PACKAGE, "package1", "ClassATest"),
                arrayOf("src", "test", language, BASE_PACKAGE, "package2", "ClassBTest")
            ),
            "com.myproject.package1.ClassA,com.myproject.package2.ClassB",
            "com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest",
            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA,com.myproject.package2.ClassB $targetTestsParam=com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest"
        ),
        Arguments.of(
            "Two Main Packages with matching Test Packages",
            listOf(
                arrayOf("src", "main", language, BASE_PACKAGE, "package1"),
                arrayOf("src", "main", language, BASE_PACKAGE, "package2")
            ),
            "com.myproject.package1.*,com.myproject.package2.*",
            "com.myproject.package1.*,com.myproject.package2.*",
            "$buildCommand $targetClassesParam=com.myproject.package1.*,com.myproject.package2.* $targetTestsParam=com.myproject.package1.*,com.myproject.package2.*"
        ),
        Arguments.of(
            "Two Test Packages with matching Main Packages",
            listOf(
                arrayOf("src", "test", language, BASE_PACKAGE, "package1"),
                arrayOf("src", "test", language, BASE_PACKAGE, "package2")
            ),
            "com.myproject.package1.*,com.myproject.package2.*",
            "com.myproject.package1.*,com.myproject.package2.*",
            "$buildCommand $targetClassesParam=com.myproject.package1.*,com.myproject.package2.* $targetTestsParam=com.myproject.package1.*,com.myproject.package2.*"
        ),
        Arguments.of(
            "Main Class and its package - only package selected",
            listOf(
                arrayOf("src", "main", language, BASE_PACKAGE, "package1", "ClassA"),
                arrayOf("src", "main", language, BASE_PACKAGE, "package1")
            ),
            "com.myproject.package1.*",
            "com.myproject.package1.*",
            "$buildCommand $targetClassesParam=com.myproject.package1.* $targetTestsParam=com.myproject.package1.*"
        ),
        Arguments.of(
            "Test Class and parent package - only package selected",
            listOf(
                arrayOf("src", "test", language, BASE_PACKAGE, "package2", "ClassBTest"),
                arrayOf("src", "test", language, BASE_PACKAGE, "package2")
            ),
            "com.myproject.package2.*",
            "com.myproject.package2.*",
            "$buildCommand $targetClassesParam=com.myproject.package2.* $targetTestsParam=com.myproject.package2.*"
        ),

        // ========================================
        // Cross Source Tests
        // ========================================
        Arguments.of(
            "Main Class and its Test Class - both selected",
            listOf(
                arrayOf("src", "main", language, BASE_PACKAGE, "package1", "ClassA"),
                arrayOf("src", "test", language, BASE_PACKAGE, "package1", "ClassATest")
            ),
            "com.myproject.package1.ClassA",
            "com.myproject.package1.ClassATest",
            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA $targetTestsParam=com.myproject.package1.ClassATest"
        ),
        Arguments.of(
            "Main Class and different Test Class - all classes selected",
            listOf(
                arrayOf("src", "main", language, BASE_PACKAGE, "package1", "ClassA"),
                arrayOf("src", "test", language, BASE_PACKAGE, "package2", "ClassBTest")
            ),
            "com.myproject.package1.ClassA,com.myproject.package2.ClassB",
            "com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest",
            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA,com.myproject.package2.ClassB $targetTestsParam=com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest"
        )
    )
}
