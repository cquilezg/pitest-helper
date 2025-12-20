package com.cquilez.pitesthelper.ui

import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.present
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.NoCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.*
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.path.IDEDataPaths
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.Starter
import com.intellij.openapi.util.SystemInfo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

/**
 * UI tests to verify that the "Run Mutation Coverage..." context menu option
 * is visible when right-clicking on projects, source folders, and classes
 * in both IntelliJ IDEA and Android Studio.
 *
 * Test projects must be created manually in src/test/testData/ directory:
 * - test-maven-simple: Simple Maven project with PITest configured
 * - test-gradle-simple: Simple Gradle project with PITest configured
 */
class ContextMenuVisibilityTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            // Initialize DI container to properly handle test failures
            di = DI {
                extend(di)
                bindSingleton<CIServer>(overrides = true) {
                    object : CIServer by NoCIServer {
                        override fun reportTestFailure(
                            testName: String,
                            message: String,
                            details: String,
                            linkToLogs: String?
                        ) {
                            fail { "$testName fails: $message. \n$details" }
                        }
                    }
                }
            }
        }

        private const val INTELLIJ_VERSION = "2025.1"
        private const val MENU_OPTION_TEXT = "Run Mutation Coverage..."
    }

    // ========================================
    // IntelliJ IDEA Tests
    // ========================================

    @Test
    fun testContextMenuVisibilityInMavenProject_IntelliJ() {
        Starter.newContext(
            testName = "contextMenuVisibilityMaven_IntelliJ",
            TestCase(
                IdeProductProvider.IC,
                LocalProjectInfo(Path("src/test/testData/sample-maven"))
            ).withVersion(INTELLIJ_VERSION)
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(1.minutes)
            ideFrame {
                projectView {
                    // Expand and right-click on the project root
                    projectViewTree.expandPath("sample-maven")
                    projectViewTree.rightClickPath("sample-maven")
                }

                // Verify that the menu option is present
                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be present in Maven project", present)
            }
        }
    }

    @Test
    fun testContextMenuVisibilityInGradleProject_IntelliJ() {
        Starter.newContext(
            testName = "contextMenuVisibilityGradle_IntelliJ",
            TestCase(
                IdeProductProvider.IC,
                LocalProjectInfo(Path("src/test/testData/test-gradle-simple"))
            ).withVersion(INTELLIJ_VERSION)
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(1.minutes)
            ideFrame {
                projectView {
                    // Expand and right-click on the project root
                    projectViewTree.expandPath("test-gradle-simple")
                    projectViewTree.rightClickPath("test-gradle-simple")
                }

                // Verify that the menu option is present
                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be present in Gradle project", present)
            }
        }
    }

    @Test
    fun testContextMenuVisibilityOnSourceFolder_IntelliJ() {
        Starter.newContext(
            testName = "contextMenuVisibilitySourceFolder_IntelliJ",
            TestCase(
                IdeProductProvider.IC,
                LocalProjectInfo(Path("src/test/testData/test-maven-simple"))
            ).withVersion(INTELLIJ_VERSION)
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(1.minutes)
            ideFrame {
                projectView {
                    // Navigate to source folder: src/main/java
                    projectViewTree.expandPath("test-maven-simple", "src", "main", "java")
                    projectViewTree.rightClickPath("test-maven-simple", "src", "main", "java")
                }

                // Verify that the menu option is present
                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be present on source folder", present)
            }
        }
    }

    @Test
    fun testContextMenuVisibilityOnPackage_IntelliJ() {
        Starter.newContext(
            testName = "contextMenuVisibilityPackage_IntelliJ",
            TestCase(
                IdeProductProvider.IC,
                LocalProjectInfo(Path("src/test/testData/test-maven-simple"))
            ).withVersion(INTELLIJ_VERSION)
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(1.minutes)
            ideFrame {
                projectView {
                    // Navigate to a package
                    projectViewTree.expandPath("test-maven-simple", "src", "main", "java", "com.example")
                    projectViewTree.rightClickPath("test-maven-simple", "src", "main", "java", "com.example")
                }

                // Verify that the menu option is present
                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be present on package", present)
            }
        }
    }

    @Test
    fun testContextMenuVisibilityOnClass_IntelliJ() {
        Starter.newContext(
            testName = "contextMenuVisibilityClass_IntelliJ",
            TestCase(
                IdeProductProvider.IC,
                LocalProjectInfo(Path("src/test/testData/test-maven-simple"))
            ).withVersion(INTELLIJ_VERSION)
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(1.minutes)
            ideFrame {
                projectView {
                    // Navigate to a specific class
                    projectViewTree.expandPath("test-maven-simple", "src", "main", "java", "com.example", "MyClass")
                    projectViewTree.rightClickPath("test-maven-simple", "src", "main", "java", "com.example", "MyClass")
                }

                // Verify that the menu option is present
                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be present on class", present)
            }
        }
    }

    // ========================================
    // Logic Validation Tests for Maven
    // ========================================

    @Test
    fun testActionLogicOnMavenProject() {
        Starter.newContext(
            testName = "actionLogicOnMavenProject",
            TestCase(
                IdeProductProvider.IC,
                LocalProjectInfo(Path("src/test/testData/sample-maven"))
            ).withVersion(INTELLIJ_VERSION)
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(1.minutes)
            ideFrame {
                projectView {
                    Thread.sleep(3600000)
                    // Right-click on the project root (BuildUnit)
                    projectViewTree.expandPath("sample-maven", fullMatch = false)
                    projectViewTree.rightClickPath("sample-maven", fullMatch = false)
                }

                // Click on the menu option
                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be present", present)
                menuOption.click()

                // Verify that the dialog opens without errors
                // The dialog title is "Mutation Coverage"
                val dialog = x(xQuery { byTitle("Mutation Coverage") })
                dialog.shouldBe("Mutation Coverage dialog should be open", present)

                // Close the dialog by pressing Cancel button
                val cancelButton = x(xQuery { byText("Cancel") })
                cancelButton.click()
            }
        }
    }

    @Test
    fun testActionLogicOnProductionSourceFolder_Maven() {
        Starter.newContext(
            testName = "actionLogicOnProductionSourceFolder_Maven",
            TestCase(
                IdeProductProvider.IC,
                LocalProjectInfo(Path("src/test/testData/sample-maven"))
            ).withVersion(INTELLIJ_VERSION)
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(1.minutes)
            ideFrame {
                projectView {
                    // Navigate to production source folder: src/main/java
                    projectViewTree.expandPath("sample-maven", "src", "main", "java")
                    projectViewTree.rightClickPath("sample-maven", "src", "main", "java")
                }

                // Click on the menu option
                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be present", present)
                menuOption.click()

                // Verify that the dialog opens
                val dialog = x(xQuery { byTitle("Mutation Coverage") })
                dialog.shouldBe("Mutation Coverage dialog should be open for production source folder", present)

                // Close the dialog
                val cancelButton = x(xQuery { byText("Cancel") })
                cancelButton.click()
            }
        }
    }

    @Test
    fun testActionLogicOnTestSourceFolder_Maven() {
        Starter.newContext(
            testName = "actionLogicOnTestSourceFolder_Maven",
            TestCase(
                IdeProductProvider.IC,
                LocalProjectInfo(Path("src/test/testData/sample-maven"))
            ).withVersion(INTELLIJ_VERSION)
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(1.minutes)
            ideFrame {
                projectView {
                    // Navigate to test source folder: src/test/java
                    projectViewTree.expandPath("sample-maven", "src", "test", "java")
                    projectViewTree.rightClickPath("sample-maven", "src", "test", "java")
                }

                // Click on the menu option
                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be present", present)
                menuOption.click()

                // Verify that the dialog opens
                val dialog = x(xQuery { byTitle("Mutation Coverage") })
                dialog.shouldBe("Mutation Coverage dialog should be open for test source folder", present)

                // Close the dialog
                val cancelButton = x(xQuery { byText("Cancel") })
                cancelButton.click()
            }
        }
    }

    @Test
    fun testActionLogicOnPackage_Maven() {
        Starter.newContext(
            testName = "actionLogicOnPackage_Maven",
            TestCase(
                IdeProductProvider.IC,
                LocalProjectInfo(Path("src/test/testData/sample-maven"))
            ).withVersion(INTELLIJ_VERSION)
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(1.minutes)
            ideFrame {
                projectView {
                    // Navigate to a package
                    projectViewTree.expandPath("sample-maven", "src", "main", "java", "com.myproject.package1")
                    projectViewTree.rightClickPath("sample-maven", "src", "main", "java", "com.myproject.package1")
                }

                // Click on the menu option
                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be present", present)
                menuOption.click()

                // Verify that the dialog opens
                val dialog = x(xQuery { byTitle("Mutation Coverage") })
                dialog.shouldBe("Mutation Coverage dialog should be open for package", present)

                // Close the dialog
                val cancelButton = x(xQuery { byText("Cancel") })
                cancelButton.click()
            }
        }
    }

    @Test
    fun testActionLogicOnClass_Maven() {
        Starter.newContext(
            testName = "actionLogicOnClass_Maven",
            TestCase(
                IdeProductProvider.IC,
                LocalProjectInfo(Path("src/test/testData/sample-maven"))
            ).withVersion(INTELLIJ_VERSION)
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(1.minutes)
            ideFrame {
                projectView {
                    // Navigate to a specific class
                    projectViewTree.expandPath("sample-maven", "src", "main", "java", "com.myproject.package1", "ClassA")
                    projectViewTree.rightClickPath("sample-maven", "src", "main", "java", "com.myproject.package1", "ClassA")
                }

                // Click on the menu option
                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be present", present)
                menuOption.click()

                // Verify that the dialog opens
                val dialog = x(xQuery { byTitle("Mutation Coverage") })
                dialog.shouldBe("Mutation Coverage dialog should be open for class", present)

                // Close the dialog
                val cancelButton = x(xQuery { byText("Cancel") })
                cancelButton.click()
            }
        }
    }

    @Test
    fun testActionLogicOnTestClass_Maven() {
        Starter.newContext(
            testName = "actionLogicOnTestClass_Maven",
            TestCase(
                IdeProductProvider.IC,
                LocalProjectInfo(Path("src/test/testData/sample-maven"))
            ).withVersion(INTELLIJ_VERSION)
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(1.minutes)
            ideFrame {
                projectView {
                    // Navigate to a test class
                    projectViewTree.expandPath("sample-maven", "src", "test", "java", "com.myproject.package1", "ClassATest")
                    projectViewTree.rightClickPath("sample-maven", "src", "test", "java", "com.myproject.package1", "ClassATest")
                }

                // Click on the menu option
                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
                menuOption.shouldBe("'$MENU_OPTION_TEXT' menu option should be present", present)
                menuOption.click()

                // Verify that the dialog opens
                val dialog = x(xQuery { byTitle("Mutation Coverage") })
                dialog.shouldBe("Mutation Coverage dialog should be open for test class", present)

                // Close the dialog
                val cancelButton = x(xQuery { byText("Cancel") })
                cancelButton.click()
            }
        }
    }
}

