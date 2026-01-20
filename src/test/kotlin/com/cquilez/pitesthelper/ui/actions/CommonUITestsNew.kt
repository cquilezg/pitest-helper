package com.cquilez.pitesthelper.ui.actions

import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.enabled
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.ui.visible
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.ide.starter.driver.engine.BackgroundRun
import org.junit.jupiter.params.provider.Arguments
import java.awt.event.KeyEvent
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream
import kotlin.time.Duration.Companion.seconds

object CommonUITestsNew {
    const val INTELLIJ_VERSION = "2025.1"
    const val MENU_OPTION_TEXT = "Run Mutation Coverage..."

    /**
     * Closes the Mutation Coverage dialog if it is open.
     * Used as @AfterEach cleanup in test classes.
     */
    fun closeMutationCoverageDialogIfOpen(run: BackgroundRun) {
        run.driver.withContext {
            ideFrame {
                val beforeGetDialogs = Instant.now()
                val dialogs = xx(xQuery { byTitle("Mutation Coverage") })
                val afterGetDialogs = Instant.now()
                println(
                    "[TIMING] afterEachHook - Get dialogs: ${
                        Duration.between(beforeGetDialogs, afterGetDialogs).toMillis()
                    } ms"
                )

                if (dialogs.list().isNotEmpty()) {
                    val dialog = dialogs.list()[0]
                    if (dialog.isVisible() && dialog.isEnabled()) {
                        val cancelButton = dialog.x(xQuery { byText("Cancel") })
                        if (cancelButton.isVisible() && cancelButton.isEnabled()) {
                            cancelButton.click()
                        }
                    }
                }

                val endOfMethod = Instant.now()
                println(
                    "[TIMING] afterEachHook - Total time: ${
                        Duration.between(beforeGetDialogs, endOfMethod).toMillis()
                    } ms"
                )
            }
        }
    }

    /**
     * Executes a single node selection test.
     */
    fun executeSingleNodeTest(
        run: BackgroundRun,
        nodePath: Array<String>,
        expectedTargetClasses: String,
        expectedTargetTests: String,
        expectedCommand: String
    ) {
        val testStartTime = Instant.now()
        println("[TIMING] testSingleNodeSelection - Started")

        run.driver.withContext {
            ideFrame {
                projectView {
                    val beforeRightClickPath = Instant.now()
                    projectViewTree.rightClickPath(*nodePath, fullMatch = false)
                    val afterRightClickPath = Instant.now()
                    println(
                        "[TIMING] testSingleNodeSelection - rightClickPath: ${
                            Duration.between(beforeRightClickPath, afterRightClickPath).toMillis()
                        } ms"
                    )
                }

                // Click on the menu option
                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be visible", visible, 3.seconds)
                menuOption.click()

                // Verify that the dialog opens
                val dialog = x(xQuery { byTitle("Mutation Coverage") })
                dialog.shouldBe("Mutation Coverage dialog should be open", visible, 3.seconds)

                // Verify Target Classes text field
                dialog.x(xQuery {
                    and(
                        byClass("JBTextField"),
                        byAccessibleName("Target Classes:")
                    )
                }).shouldBe("Target Classes field should be visible", visible, 3.seconds)
                    .hasText(expectedTargetClasses)

                // Verify Target Tests text field
                dialog.x(xQuery {
                    and(
                        byClass("JBTextField"),
                        byAccessibleName("Target Tests:")
                    )
                }).shouldBe("Target Tests field should be visible", visible, 3.seconds)
                    .hasText(expectedTargetTests)

                // Verify the command text area contains the expected command
                val commandTextArea = dialog.x(xQuery { byClass("JBTextArea") })
                commandTextArea.shouldBe("Command text area should be visible", visible, 3.seconds)
                commandTextArea.hasText(expectedCommand)

                // Close the dialog
                val cancelButton = dialog.x(xQuery { byText("Cancel") })
                cancelButton.click()
                val afterCancelClick = Instant.now()
                println(
                    "[TIMING] testSingleNodeSelection - After cancelButton.click(): ${
                        Duration.between(testStartTime, afterCancelClick).toMillis()
                    } ms elapsed"
                )
            }
        }

        val testEndTime = Instant.now()
        println(
            "[TIMING] testSingleNodeSelection - Total test time: ${
                Duration.between(testStartTime, testEndTime).toMillis()
            } ms"
        )
    }

    /**
     * Executes a multi node selection test.
     */
    fun executeMultiNodeTest(
        run: BackgroundRun,
        nodePaths: List<Array<String>>,
        expectedTargetClasses: String,
        expectedTargetTests: String,
        expectedCommand: String
    ) {
        val testStartTime = Instant.now()
        println("[TIMING] testMultiNodeSelection - Started")

        run.driver.withContext {
            ideFrame {
                projectView {
                    val beforeMultiSelect = Instant.now()

                    // Select multiple nodes using Ctrl+click
                    keyboard {
                        pressing(KeyEvent.VK_CONTROL) {
                            // Click all paths except the last one
                            for (i in 0 until nodePaths.size - 1) {
                                projectViewTree.clickPath(*nodePaths[i], fullMatch = false)
                            }
                        }
                        projectViewTree.rightClickPath(*nodePaths.last(), fullMatch = false)
                    }

                    val afterMultiSelect = Instant.now()
                    println(
                        "[TIMING] testMultiNodeSelection - Multi-select with Ctrl+click: ${
                            Duration.between(beforeMultiSelect, afterMultiSelect).toMillis()
                        } ms"
                    )
                }

                // Click on the menu option
                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be visible", visible, 3.seconds)
                menuOption.click()

                // Verify that the dialog opens
                val dialog = x(xQuery { byTitle("Mutation Coverage") })
                dialog.shouldBe("Mutation Coverage dialog should be open", visible, 3.seconds)

                // Verify Target Classes text field
                dialog.x(xQuery {
                    and(
                        byClass("JBTextField"),
                        byAccessibleName("Target Classes:")
                    )
                }).shouldBe("Target Classes field should be visible", visible, 3.seconds)
                    .hasText(expectedTargetClasses)

                // Verify Target Tests text field
                dialog.x(xQuery {
                    and(
                        byClass("JBTextField"),
                        byAccessibleName("Target Tests:")
                    )
                }).shouldBe("Target Tests field should be visible", visible, 3.seconds)
                    .hasText(expectedTargetTests)

                // Verify the command text area contains the expected command
                val commandTextArea = dialog.x(xQuery { byClass("JBTextArea") })
                commandTextArea.shouldBe("Command text area should be visible", visible, 3.seconds)
                commandTextArea.hasText(expectedCommand)

                // Close the dialog
                val cancelButton = dialog.x(xQuery { byText("Cancel") })
                cancelButton.click()
                val afterCancelClick = Instant.now()
                println(
                    "[TIMING] testMultiNodeSelection - After cancelButton.click(): ${
                        Duration.between(testStartTime, afterCancelClick).toMillis()
                    } ms elapsed"
                )
            }
        }

        val testEndTime = Instant.now()
        println(
            "[TIMING] testMultiNodeSelection - Total test time: ${
                Duration.between(testStartTime, testEndTime).toMillis()
            } ms"
        )
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
        ),
        Arguments.of(
            "Single Main Package with matching Test Package",
            arrayOf(projectName, "src", "main", language, "com.myproject", "package2"),
            "com.myproject.package2.*",
            "com.myproject.package2.*",
            "$buildCommand $targetClassesParam=com.myproject.package2.* $targetTestsParam=com.myproject.package2.*"
        ),
        Arguments.of(
            "Single Test Class with matching Main Class",
            arrayOf(projectName, "src", "test", language, "com.myproject", "package2", "ClassBTest"),
            "com.myproject.package2.ClassB",
            "com.myproject.package2.ClassBTest",
            "$buildCommand $targetClassesParam=com.myproject.package2.ClassB $targetTestsParam=com.myproject.package2.ClassBTest"
        ),
        Arguments.of(
            "Single Test Package with matching Main Package",
            arrayOf(projectName, "src", "test", language, "com.myproject", "package1"),
            "com.myproject.package1.*",
            "com.myproject.package1.*",
            "$buildCommand $targetClassesParam=com.myproject.package1.* $targetTestsParam=com.myproject.package1.*"
        ),

        // ========================================
        // Special Cases Tests
        // ========================================
        Arguments.of(
            "Main Class with multiple test candidates - selects test in same package",
            arrayOf(projectName, "src", "main", language, "com.myproject", "package1", "ClassC"),
            "com.myproject.package1.ClassC",
            "com.myproject.package1.ClassCTest",
            "$buildCommand $targetClassesParam=com.myproject.package1.ClassC $targetTestsParam=com.myproject.package1.ClassCTest"
        ),
        Arguments.of(
            "Main Class with test in superior package",
            arrayOf(projectName, "src", "main", language, "com.myproject", "package3.impl", "ClassC"),
            "com.myproject.package3.impl.ClassC",
            "com.myproject.package3.ClassCTest",
            "$buildCommand $targetClassesParam=com.myproject.package3.impl.ClassC $targetTestsParam=com.myproject.package3.ClassCTest"
        )
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
        Arguments.of(
            "Two Main Classes with matching Test Classes",
            listOf(
                arrayOf(projectName, "src", "main", language, "com.myproject", "package1", "ClassA"),
                arrayOf(projectName, "src", "main", language, "com.myproject", "package1", "ClassD")
            ),
            "com.myproject.package1.ClassA,com.myproject.package1.ClassD",
            "com.myproject.package1.ClassATest,com.myproject.package1.ClassDTest",
            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA,com.myproject.package1.ClassD $targetTestsParam=com.myproject.package1.ClassATest,com.myproject.package1.ClassDTest"
        ),
        Arguments.of(
            "Two Test Classes with matching Main Classes",
            listOf(
                arrayOf(projectName, "src", "test", language, "com.myproject", "package1", "ClassATest"),
                arrayOf(projectName, "src", "test", language, "com.myproject", "package2", "ClassBTest")
            ),
            "com.myproject.package1.ClassA,com.myproject.package2.ClassB",
            "com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest",
            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA,com.myproject.package2.ClassB $targetTestsParam=com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest"
        ),
        Arguments.of(
            "Two Main Packages with matching Test Packages",
            listOf(
                arrayOf(projectName, "src", "main", language, "com.myproject", "package1"),
                arrayOf(projectName, "src", "main", language, "com.myproject", "package2")
            ),
            "com.myproject.package1.*,com.myproject.package2.*",
            "com.myproject.package1.*,com.myproject.package2.*",
            "$buildCommand $targetClassesParam=com.myproject.package1.*,com.myproject.package2.* $targetTestsParam=com.myproject.package1.*,com.myproject.package2.*"
        ),
        Arguments.of(
            "Two Test Packages with matching Main Packages",
            listOf(
                arrayOf(projectName, "src", "test", language, "com.myproject", "package1"),
                arrayOf(projectName, "src", "test", language, "com.myproject", "package2")
            ),
            "com.myproject.package1.*,com.myproject.package2.*",
            "com.myproject.package1.*,com.myproject.package2.*",
            "$buildCommand $targetClassesParam=com.myproject.package1.*,com.myproject.package2.* $targetTestsParam=com.myproject.package1.*,com.myproject.package2.*"
        ),
        Arguments.of(
            "Main Class and its package - only package selected",
            listOf(
                arrayOf(projectName, "src", "main", language, "com.myproject", "package1", "ClassA"),
                arrayOf(projectName, "src", "main", language, "com.myproject", "package1")
            ),
            "com.myproject.package1.*",
            "com.myproject.package1.*",
            "$buildCommand $targetClassesParam=com.myproject.package1.* $targetTestsParam=com.myproject.package1.*"
        ),
        Arguments.of(
            "Test Class and parent package - only package selected",
            listOf(
                arrayOf(projectName, "src", "test", language, "com.myproject", "package2", "ClassBTest"),
                arrayOf(projectName, "src", "test", language, "com.myproject", "package2")
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
                arrayOf(projectName, "src", "main", language, "com.myproject", "package1", "ClassA"),
                arrayOf(projectName, "src", "test", language, "com.myproject", "package1", "ClassATest")
            ),
            "com.myproject.package1.ClassA",
            "com.myproject.package1.ClassATest",
            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA $targetTestsParam=com.myproject.package1.ClassATest"
        ),
        Arguments.of(
            "Main Class and different Test Class - all classes selected",
            listOf(
                arrayOf(projectName, "src", "main", language, "com.myproject", "package1", "ClassA"),
                arrayOf(projectName, "src", "test", language, "com.myproject", "package2", "ClassBTest")
            ),
            "com.myproject.package1.ClassA,com.myproject.package2.ClassB",
            "com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest",
            "$buildCommand $targetClassesParam=com.myproject.package1.ClassA,com.myproject.package2.ClassB $targetTestsParam=com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest"
        )
    )
}
