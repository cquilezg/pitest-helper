package com.cquilez.pitesthelper.ui.actions

import com.intellij.driver.sdk.ui.components.UiComponent
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.JTreeUiComponent
import com.intellij.driver.sdk.ui.components.elements.dialog
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.ui.visible
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.ide.starter.driver.engine.BackgroundRun
import org.junit.jupiter.params.provider.Arguments
import java.awt.event.KeyEvent
import java.util.stream.Stream
import kotlin.time.Duration.Companion.seconds

/**
 * Gets the actual project root node name from the tree.
 * The root node may contain additional path information (e.g., "sample-maven ~/path/to/project"),
 * so we search for a root node that contains the expected project name.
 */
fun JTreeUiComponent.getActualProjectRoot(expectedName: String): String {
    return collectExpandedPaths()
        .firstOrNull { it.path.size == 1 && it.path[0].contains(expectedName, ignoreCase = true) }
        ?.path?.get(0)
        ?: throw JTreeUiComponent.PathNotFoundException("Root node containing '$expectedName' not found")
}

/**
 * Fast click on a tree path by using the native fixture methods.
 * This bypasses the slow Kotlin-level expansion logic with 1-second waits.
 *
 * @param projectName The expected project name (used to find the actual root node)
 * @param restOfPath The remaining path elements after the project root
 */
fun JTreeUiComponent.fastClickPath(projectName: String, vararg restOfPath: String) {
//    val actualRoot = getActualProjectRoot(projectName)
    val sep = fixture.separator()
    val fullPath = (listOf(projectName) + restOfPath.toList()).joinToString(sep)
    fixture.clickPath(fullPath)
}

/**
 * Fast right-click on a tree path by using the native fixture methods.
 * This bypasses the slow Kotlin-level expansion logic with 1-second waits.
 *
 * @param projectName The expected project name (used to find the actual root node)
 * @param restOfPath The remaining path elements after the project root
 */
fun JTreeUiComponent.fastRightClickPath(projectName: String, vararg restOfPath: String) {
    val actualRoot = getActualProjectRoot(projectName)
    val sep = fixture.separator()
    val fullPath = (listOf(actualRoot) + restOfPath.toList()).joinToString(sep)
    fixture.rightClickPath(fullPath)
}

object CommonUITestsNew {
    const val INTELLIJ_VERSION = "2025.1"
    const val MENU_OPTION_TEXT = "Run Mutation Coverage..."

    private val SOFT_WRAP_CHARS = Regex("[⤦⤥]")

    private fun stripSoftWrapChars(text: String): String = text.replace(SOFT_WRAP_CHARS, "")

    fun UiComponent.waitForEditorTextFieldWithText(
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
                    val dialog = dialogs.list()[0]
                    if (dialog.isVisible() && dialog.isEnabled()) {
                        val cancelButton = dialog.x(xQuery { byText("Cancel") })
                        if (cancelButton.isVisible() && cancelButton.isEnabled()) {
                            cancelButton.click()
                        }
                    }
                }
            }
        }
    }

    fun executeSingleNodeTest(
        run: BackgroundRun,
        nodePath: Array<String>,
        expectedTargetClasses: String,
        expectedTargetTests: String,
        expectedCommand: String
    ) {
        run.driver.withContext {
            ideFrame {
                projectView {
                    val projectName = nodePath[0]
                    val restOfPath = nodePath.sliceArray(1 until nodePath.size)
                    projectViewTree.clickPath(projectName, *restOfPath)
                    projectViewTree.rightClickPath(projectName, *restOfPath)
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
        run.driver.withContext {
            ideFrame {
                projectView {
                    val firstPath = nodePaths[0]
                    val texts = getAllTexts()
                    val projectNode = "${texts[0].toString().trim()} ${texts[1].toString().trim()}"
                    projectViewTree.clickPath(projectNode, *firstPath.sliceArray(1 until firstPath.size))
                    keyboard {
                        pressing(KeyEvent.VK_CONTROL) {
                            for (i in 1 until nodePaths.size) {
                                val path = nodePaths[i]
                                projectViewTree.fastClickPath(path[0], *path.sliceArray(1 until path.size))
                            }
                        }
                    }
                    val lastPath = nodePaths.last()
                    projectViewTree.fastRightClickPath(lastPath[0], *lastPath.sliceArray(1 until lastPath.size))
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
        menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be visible", visible, 3.seconds)
        menuOption.click()

        dialog(title = "Mutation Coverage") {
            shouldBe("Mutation Coverage dialog should be open", visible, 3.seconds)

            x(xQuery {
                and(
                    byClass("JBTextField"),
                    byAccessibleName("Target Classes:")
                )
            }).shouldBe("Target Classes field should be visible", visible, 3.seconds)
                .waitForEditorTextFieldWithText(
                    expectedTargetClasses,
                    "Target Classes should have text: $expectedTargetClasses",
                    3.seconds
                )

            x(xQuery {
                and(
                    byClass("JBTextField"),
                    byAccessibleName("Target Tests:")
                )
            }).shouldBe("Target Tests field should be visible", visible, 3.seconds)
                .waitForEditorTextFieldWithText(
                    expectedTargetTests,
                    "Target Tests should have text: $expectedCommand",
                    3.seconds
                )

            x(xQuery {
                byClass("EditorTextField")
            }).shouldBe("Command text area should be visible", visible, 3.seconds)
                .waitForEditorTextFieldWithText(
                    expectedCommand,
                    "Command text area should have text: $expectedCommand",
                    3.seconds
                )
        }.closeDialog()
    }

    // ========================================
    // Maven Test Cases (using -D prefix)
    // ========================================

    @JvmStatic
    fun singleNodeTestCases(projectName: String, language: String, buildCommand: String): Stream<Arguments> =
        singleNodeTestCases(projectName, language, buildCommand, "-DtargetClasses", "-DtargetTests")

    @JvmStatic
    fun multiNodeTestCases(projectName: String, language: String, buildCommand: String): Stream<Arguments> =
        multiNodeTestCases(projectName, language, buildCommand, "-DtargetClasses", "-DtargetTests")

    // ========================================
    // Gradle Test Cases (using -Ppitest prefix)
    // ========================================

    @JvmStatic
    fun gradleSingleNodeTestCases(projectName: String, language: String, buildCommand: String): Stream<Arguments> =
        singleNodeTestCases(projectName, language, buildCommand, "-Ppitest.targetClasses", "-Ppitest.targetTests")

    @JvmStatic
    fun gradleMultiNodeTestCases(projectName: String, language: String, buildCommand: String): Stream<Arguments> =
        multiNodeTestCases(projectName, language, buildCommand, "-Ppitest.targetClasses", "-Ppitest.targetTests")

    // ========================================
    // Gradle Multi-Module Test Cases
    // ========================================

    @JvmStatic
    fun gradleMultiModuleSingleNodeTestCases(projectName: String, language: String): Stream<Arguments> = Stream.of(
        // ========================================
        // Single Node Tests
        // ========================================
        Arguments.of(
            "Single Main Class with matching Test Class (lib module)",
            arrayOf(projectName, "lib", "src", "main", language, "com.myproject", "package1", "ClassA"),
            "com.myproject.package1.ClassA",
            "com.myproject.package1.ClassATest",
            "gradle :lib:pitest -Ppitest.targetClasses=com.myproject.package1.ClassA -Ppitest.targetTests=com.myproject.package1.ClassATest"
        ),
        Arguments.of(
            "Single Main Package with matching Test Package (app module)",
            arrayOf(projectName, "app", "src", "main", language, "com.myproject", "package2"),
            "com.myproject.package2.*",
            "com.myproject.package2.*",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package2.* -Ppitest.targetTests=com.myproject.package2.*"
        ),
        Arguments.of(
            "Single Test Class with matching Main Class (app module)",
            arrayOf(projectName, "app", "src", "test", language, "com.myproject", "package2", "ClassBTest"),
            "com.myproject.package2.ClassB",
            "com.myproject.package2.ClassBTest",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package2.ClassB -Ppitest.targetTests=com.myproject.package2.ClassBTest"
        ),
        Arguments.of(
            "Single Test Package with matching Main Package (lib module)",
            arrayOf(projectName, "lib", "src", "test", language, "com.myproject", "package1"),
            "com.myproject.package1.*",
            "com.myproject.package1.*",
            "gradle :lib:pitest -Ppitest.targetClasses=com.myproject.package1.* -Ppitest.targetTests=com.myproject.package1.*"
        ),

        // ========================================
        // Special Cases Tests
        // ========================================
        Arguments.of(
            "Main Class with multiple test candidates - selects test in same package (lib module)",
            arrayOf(projectName, "lib", "src", "main", language, "com.myproject", "package1", "ClassC"),
            "com.myproject.package1.ClassC",
            "com.myproject.package1.ClassCTest",
            "gradle :lib:pitest -Ppitest.targetClasses=com.myproject.package1.ClassC -Ppitest.targetTests=com.myproject.package1.ClassCTest"
        ),
        Arguments.of(
            "Main Class with test in superior package (app module)",
            arrayOf(projectName, "app", "src", "main", language, "com.myproject", "package3.impl", "ClassC"),
            "com.myproject.package3.impl.ClassC",
            "com.myproject.package3.ClassCTest",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package3.impl.ClassC -Ppitest.targetTests=com.myproject.package3.ClassCTest"
        )
    )

    @JvmStatic
    fun gradleMultiModuleMultiNodeTestCases(projectName: String, language: String): Stream<Arguments> = Stream.of(
        // ========================================
        // Multi Node Tests
        // ========================================
        Arguments.of(
            "Two Main Classes with matching Test Classes (app module)",
            listOf(
                arrayOf(projectName, "app", "src", "main", language, "com.myproject", "package1", "ClassA"),
                arrayOf(projectName, "app", "src", "main", language, "com.myproject", "package1", "ClassD")
            ),
            "com.myproject.package1.ClassA,com.myproject.package1.ClassD",
            "com.myproject.package1.ClassATest,com.myproject.package1.ClassDTest",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package1.ClassA,com.myproject.package1.ClassD -Ppitest.targetTests=com.myproject.package1.ClassATest,com.myproject.package1.ClassDTest"
        ),
        Arguments.of(
            "Two Test Classes with matching Main Classes (lib module)",
            listOf(
                arrayOf(projectName, "lib", "src", "test", language, "com.myproject", "package1", "ClassATest"),
                arrayOf(projectName, "lib", "src", "test", language, "com.myproject", "package2", "ClassBTest")
            ),
            "com.myproject.package1.ClassA,com.myproject.package2.ClassB",
            "com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest",
            "gradle :lib:pitest -Ppitest.targetClasses=com.myproject.package1.ClassA,com.myproject.package2.ClassB -Ppitest.targetTests=com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest"
        ),
        Arguments.of(
            "Two Main Packages with matching Test Packages (app module)",
            listOf(
                arrayOf(projectName, "app", "src", "main", language, "com.myproject", "package1"),
                arrayOf(projectName, "app", "src", "main", language, "com.myproject", "package2")
            ),
            "com.myproject.package1.*,com.myproject.package2.*",
            "com.myproject.package1.*,com.myproject.package2.*",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package1.*,com.myproject.package2.* -Ppitest.targetTests=com.myproject.package1.*,com.myproject.package2.*"
        ),
        Arguments.of(
            "Two Test Packages with matching Main Packages (app module)",
            listOf(
                arrayOf(projectName, "app", "src", "test", language, "com.myproject", "package1"),
                arrayOf(projectName, "app", "src", "test", language, "com.myproject", "package2")
            ),
            "com.myproject.package1.*,com.myproject.package2.*",
            "com.myproject.package1.*,com.myproject.package2.*",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package1.*,com.myproject.package2.* -Ppitest.targetTests=com.myproject.package1.*,com.myproject.package2.*"
        ),
        Arguments.of(
            "Main Class and its package - only package selected (lib module)",
            listOf(
                arrayOf(projectName, "lib", "src", "main", language, "com.myproject", "package1", "ClassA"),
                arrayOf(projectName, "lib", "src", "main", language, "com.myproject", "package1")
            ),
            "com.myproject.package1.*",
            "com.myproject.package1.*",
            "gradle :lib:pitest -Ppitest.targetClasses=com.myproject.package1.* -Ppitest.targetTests=com.myproject.package1.*"
        ),
        Arguments.of(
            "Test Class and parent package - only package selected (lib module)",
            listOf(
                arrayOf(projectName, "lib", "src", "test", language, "com.myproject", "package2", "ClassBTest"),
                arrayOf(projectName, "lib", "src", "test", language, "com.myproject", "package2")
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
                arrayOf(projectName, "app", "src", "main", language, "com.myproject", "package1", "ClassA"),
                arrayOf(projectName, "app", "src", "test", language, "com.myproject", "package1", "ClassATest")
            ),
            "com.myproject.package1.ClassA",
            "com.myproject.package1.ClassATest",
            "gradle :app:pitest -Ppitest.targetClasses=com.myproject.package1.ClassA -Ppitest.targetTests=com.myproject.package1.ClassATest"
        ),
        Arguments.of(
            "Main Class and different Test Class - all classes selected (lib module)",
            listOf(
                arrayOf(projectName, "lib", "src", "main", language, "com.myproject", "package1", "ClassA"),
                arrayOf(projectName, "lib", "src", "test", language, "com.myproject", "package2", "ClassBTest")
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
        projectName: String,
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
            arrayOf(projectName, "src", "main", language, "com.myproject", "package1", "ClassA"),
            "com.myproject.package1.ClassA",
            "com.myproject.package1.ClassATest",
            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA $targetTestsParam=com.myproject.package1.ClassATest"
        )
//        ,
//        Arguments.of(
//            "Single Main Package with matching Test Package",
//            arrayOf(projectName, "src", "main", language, "com.myproject", "package2"),
//            "com.myproject.package2.*",
//            "com.myproject.package2.*",
//            "$buildCommand $targetClassesParam=com.myproject.package2.* $targetTestsParam=com.myproject.package2.*"
//        ),
//        Arguments.of(
//            "Single Test Class with matching Main Class",
//            arrayOf(projectName, "src", "test", language, "com.myproject", "package2", "ClassBTest"),
//            "com.myproject.package2.ClassB",
//            "com.myproject.package2.ClassBTest",
//            "$buildCommand $targetClassesParam=com.myproject.package2.ClassB $targetTestsParam=com.myproject.package2.ClassBTest"
//        ),
//        Arguments.of(
//            "Single Test Package with matching Main Package",
//            arrayOf(projectName, "src", "test", language, "com.myproject", "package1"),
//            "com.myproject.package1.*",
//            "com.myproject.package1.*",
//            "$buildCommand $targetClassesParam=com.myproject.package1.* $targetTestsParam=com.myproject.package1.*"
//        ),
//
//        // ========================================
//        // Special Cases Tests
//        // ========================================
//        Arguments.of(
//            "Main Class with multiple test candidates - selects test in same package",
//            arrayOf(projectName, "src", "main", language, "com.myproject", "package1", "ClassC"),
//            "com.myproject.package1.ClassC",
//            "com.myproject.package1.ClassCTest",
//            "$buildCommand $targetClassesParam=com.myproject.package1.ClassC $targetTestsParam=com.myproject.package1.ClassCTest"
//        ),
//        Arguments.of(
//            "Main Class with test in superior package",
//            arrayOf(projectName, "src", "main", language, "com.myproject", "package3.impl", "ClassC"),
//            "com.myproject.package3.impl.ClassC",
//            "com.myproject.package3.ClassCTest",
//            "$buildCommand $targetClassesParam=com.myproject.package3.impl.ClassC $targetTestsParam=com.myproject.package3.ClassCTest"
//        )
    )

    @JvmStatic
    fun multiNodeTestCases(
        projectName: String,
        language: String,
        buildCommand: String,
        targetClassesParam: String,
        targetTestsParam: String
    ): Stream<Arguments> = Stream.of(
        // ========================================
        // Multi Node Tests
        // ========================================
//        Arguments.of(
//            "Two Main Classes with matching Test Classes",
//            listOf(
//                arrayOf(projectName, "src", "main", language, "com.myproject", "package1", "ClassA"),
//                arrayOf(projectName, "src", "main", language, "com.myproject", "package1", "ClassD")
//            ),
//            "com.myproject.package1.ClassA,com.myproject.package1.ClassD",
//            "com.myproject.package1.ClassATest,com.myproject.package1.ClassDTest",
//            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA,com.myproject.package1.ClassD $targetTestsParam=com.myproject.package1.ClassATest,com.myproject.package1.ClassDTest"
//        ),
//        Arguments.of(
//            "Two Test Classes with matching Main Classes",
//            listOf(
//                arrayOf(projectName, "src", "test", language, "com.myproject", "package1", "ClassATest"),
//                arrayOf(projectName, "src", "test", language, "com.myproject", "package2", "ClassBTest")
//            ),
//            "com.myproject.package1.ClassA,com.myproject.package2.ClassB",
//            "com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest",
//            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA,com.myproject.package2.ClassB $targetTestsParam=com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest"
//        ),
//        Arguments.of(
//            "Two Main Packages with matching Test Packages",
//            listOf(
//                arrayOf(projectName, "src", "main", language, "com.myproject", "package1"),
//                arrayOf(projectName, "src", "main", language, "com.myproject", "package2")
//            ),
//            "com.myproject.package1.*,com.myproject.package2.*",
//            "com.myproject.package1.*,com.myproject.package2.*",
//            "$buildCommand $targetClassesParam=com.myproject.package1.*,com.myproject.package2.* $targetTestsParam=com.myproject.package1.*,com.myproject.package2.*"
//        ),
//        Arguments.of(
//            "Two Test Packages with matching Main Packages",
//            listOf(
//                arrayOf(projectName, "src", "test", language, "com.myproject", "package1"),
//                arrayOf(projectName, "src", "test", language, "com.myproject", "package2")
//            ),
//            "com.myproject.package1.*,com.myproject.package2.*",
//            "com.myproject.package1.*,com.myproject.package2.*",
//            "$buildCommand $targetClassesParam=com.myproject.package1.*,com.myproject.package2.* $targetTestsParam=com.myproject.package1.*,com.myproject.package2.*"
//        ),
//        Arguments.of(
//            "Main Class and its package - only package selected",
//            listOf(
//                arrayOf(projectName, "src", "main", language, "com.myproject", "package1", "ClassA"),
//                arrayOf(projectName, "src", "main", language, "com.myproject", "package1")
//            ),
//            "com.myproject.package1.*",
//            "com.myproject.package1.*",
//            "$buildCommand $targetClassesParam=com.myproject.package1.* $targetTestsParam=com.myproject.package1.*"
//        ),
//        Arguments.of(
//            "Test Class and parent package - only package selected",
//            listOf(
//                arrayOf(projectName, "src", "test", language, "com.myproject", "package2", "ClassBTest"),
//                arrayOf(projectName, "src", "test", language, "com.myproject", "package2")
//            ),
//            "com.myproject.package2.*",
//            "com.myproject.package2.*",
//            "$buildCommand $targetClassesParam=com.myproject.package2.* $targetTestsParam=com.myproject.package2.*"
//        ),
//
//        // ========================================
//        // Cross Source Tests
//        // ========================================
//        Arguments.of(
//            "Main Class and its Test Class - both selected",
//            listOf(
//                arrayOf(projectName, "src", "main", language, "com.myproject", "package1", "ClassA"),
//                arrayOf(projectName, "src", "test", language, "com.myproject", "package1", "ClassATest")
//            ),
//            "com.myproject.package1.ClassA",
//            "com.myproject.package1.ClassATest",
//            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA $targetTestsParam=com.myproject.package1.ClassATest"
//        ),
        Arguments.of(
            "Main Class and different Test Class - all classes selected",
            listOf(
                arrayOf("src", "main", language, "com.myproject", "package1", "ClassA"),
                arrayOf("src", "test", language, "com.myproject", "package2", "ClassBTest")
            ),
            "com.myproject.package1.ClassA,com.myproject.package2.ClassB",
            "com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest",
            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA,com.myproject.package2.ClassB $targetTestsParam=com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest"
        )
    )
}
