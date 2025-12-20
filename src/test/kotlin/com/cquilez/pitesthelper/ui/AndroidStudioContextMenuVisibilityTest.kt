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
import com.intellij.ide.starter.ide.InstalledIde
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.io.File
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

/**
 * UI tests for Android Studio specifically.
 *
 * These tests require Android Studio to be installed locally and will execute in the actual Android Studio IDE.
 *
 * ## Configuration
 *
 * Set the Android Studio path using either:
 * - System property: `-Dandroid.studio.path=/path/to/android/studio`
 * - Environment variable: `ANDROID_STUDIO_HOME=/path/to/android/studio`
 *
 * ## Common Android Studio Paths
 *
 * - **macOS**: `/Applications/Android Studio.app/Contents`
 * - **Linux**: `/opt/android-studio` or `~/android-studio`
 * - **Windows**: `C:\Program Files\Android\Android Studio`
 *
 * ## Running Tests
 *
 * ```bash
 * # Run all Android Studio tests
 * ./gradlew test --tests "AndroidStudioContextMenuVisibilityTest" \
 *   -Dandroid.studio.path="/Applications/Android Studio.app/Contents"
 *
 * # Run specific test
 * ./gradlew test --tests "AndroidStudioContextMenuVisibilityTest.testContextMenuVisibilityInAndroidProject_AndroidStudio" \
 *   -Dandroid.studio.path="/path/to/android/studio"
 * ```
 *
 * ## Behavior
 *
 * If `android.studio.path` is not set or the path doesn't exist, tests will be **skipped** automatically
 * (they won't fail). This allows running the full test suite without Android Studio.
 *
 * Test projects must be created manually in `src/test/testData/`:
 * - **MyApplication2**: Android project with Gradle and PITest configured
 */
class AndroidStudioContextMenuVisibilityTest {

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

        private const val MENU_OPTION_TEXT = "Run Mutation Coverage..."

        /**
         * Gets the Android Studio installation directory from system property or environment variable.
         *
         * Priority:
         * 1. System property: `android.studio.path`
         * 2. Environment variable: `ANDROID_STUDIO_HOME`
         *
         * @return File pointing to Android Studio installation, or null if not configured or doesn't exist
         */
        private fun getAndroidStudioPath(): File? {
            val path = System.getProperty("android.studio.path")
                ?: System.getenv("ANDROID_STUDIO_HOME")

            if (path.isNullOrBlank()) {
                println("⚠️  Android Studio path not configured. Set -Dandroid.studio.path or ANDROID_STUDIO_HOME")
                return null
            }

            val file = File(path)
            if (!file.exists()) {
                println("⚠️  Android Studio path does not exist: $path")
                return null
            }

            if (!file.isDirectory) {
                println("⚠️  Android Studio path is not a directory: $path")
                return null
            }

            println("✅ Using Android Studio at: $path")
            return file
        }
    }

//    @Test
//    fun testContextMenuVisibilityInAndroidProject_AndroidStudio() {
//        val androidStudioDir = getAndroidStudioPath()
//        assumeTrue(
//            androidStudioDir != null,
//            "Android Studio not configured. Set -Dandroid.studio.path=/path/to/android/studio or ANDROID_STUDIO_HOME environment variable"
//        )
//
//        Starter.newContext(
//            testName = "contextMenuVisibilityAndroid_AndroidStudio",
//            TestCase(
//                ideInfo = InstalledIde(
//                    installationDirectory = androidStudioDir!!.toPath(),
//                    buildNumber = null // Auto-detect build number from installation
//                ),
//                projectInfo = LocalProjectInfo(Path("src/test/testData/MyApplication2"))
//            )
//        ).apply {
//            val pathToPlugin = System.getProperty("path.to.build.plugin")
//            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
//        }.runIdeWithDriver().useDriverAndCloseIde {
//            waitForIndicators(2.minutes) // Android Studio may take longer to load
//            ideFrame {
//                projectView {
//                    // Expand and right-click on the Android project root
//                    projectViewTree.expandPath("MyApplication2")
//                    projectViewTree.rightClickPath("MyApplication2")
//                }
//
//                // Verify that the menu option is present in real Android Studio
//                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
//                menuOption.shouldBe(
//                    "'$MENU_OPTION_TEXT' menu option should be present in Android Studio on project root",
//                    present
//                )
//            }
//        }
//    }
//
//    @Test
//    fun testContextMenuVisibilityOnSourceFolder_AndroidStudio() {
//        val androidStudioDir = getAndroidStudioPath()
//        assumeTrue(
//            androidStudioDir != null,
//            "Android Studio not configured. Set -Dandroid.studio.path=/path/to/android/studio"
//        )
//
//        Starter.newContext(
//            testName = "contextMenuVisibilitySourceFolder_AndroidStudio",
//            TestCase(
//                ideInfo = InstalledIde(
//                    installationDirectory = androidStudioDir!!.toPath(),
//                    buildNumber = null
//                ),
//                projectInfo = LocalProjectInfo(Path("src/test/testData/MyApplication2"))
//            )
//        ).apply {
//            val pathToPlugin = System.getProperty("path.to.build.plugin")
//            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
//        }.runIdeWithDriver().useDriverAndCloseIde {
//            waitForIndicators(2.minutes)
//            ideFrame {
//                projectView {
//                    // Navigate to source folder in Android project: app/src/main/java
//                    projectViewTree.expandPath("MyApplication2", "app", "src", "main", "java")
//                    projectViewTree.rightClickPath("MyApplication2", "app", "src", "main", "java")
//                }
//
//                // Verify that the menu option is present
//                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
//                menuOption.shouldBe(
//                    "'$MENU_OPTION_TEXT' menu option should be present in Android Studio on source folder",
//                    present
//                )
//            }
//        }
//    }
//
//    @Test
//    fun testContextMenuVisibilityOnPackage_AndroidStudio() {
//        val androidStudioDir = getAndroidStudioPath()
//        assumeTrue(
//            androidStudioDir != null,
//            "Android Studio not configured. Set -Dandroid.studio.path=/path/to/android/studio"
//        )
//
//        Starter.newContext(
//            testName = "contextMenuVisibilityPackage_AndroidStudio",
//            TestCase(
//                ideInfo = InstalledIde(
//                    installationDirectory = androidStudioDir!!.toPath(),
//                    buildNumber = null
//                ),
//                projectInfo = LocalProjectInfo(Path("src/test/testData/MyApplication2"))
//            )
//        ).apply {
//            val pathToPlugin = System.getProperty("path.to.build.plugin")
//            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
//        }.runIdeWithDriver().useDriverAndCloseIde {
//            waitForIndicators(2.minutes)
//            ideFrame {
//                projectView {
//                    // Navigate to a package in Android project
//                    projectViewTree.expandPath(
//                        "MyApplication2",
//                        "app",
//                        "src",
//                        "main",
//                        "java",
//                        "com.example.myapplication2"
//                    )
//                    projectViewTree.rightClickPath(
//                        "MyApplication2",
//                        "app",
//                        "src",
//                        "main",
//                        "java",
//                        "com.example.myapplication2"
//                    )
//                }
//
//                // Verify that the menu option is present
//                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
//                menuOption.shouldBe(
//                    "'$MENU_OPTION_TEXT' menu option should be present in Android Studio on package",
//                    present
//                )
//            }
//        }
//    }
//
//    @Test
//    fun testContextMenuVisibilityOnClass_AndroidStudio() {
//        val androidStudioDir = getAndroidStudioPath()
//        assumeTrue(
//            androidStudioDir != null,
//            "Android Studio not configured. Set -Dandroid.studio.path=/path/to/android/studio"
//        )
//
//        Starter.newContext(
//            testName = "contextMenuVisibilityClass_AndroidStudio",
//            TestCase(
//                ideInfo = InstalledIde(
//                    installationDirectory = androidStudioDir!!.toPath(),
//                    buildNumber = null
//                ),
//                projectInfo = LocalProjectInfo(Path("src/test/testData/MyApplication2"))
//            )
//        ).apply {
//            val pathToPlugin = System.getProperty("path.to.build.plugin")
//            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
//        }.runIdeWithDriver().useDriverAndCloseIde {
//            waitForIndicators(2.minutes)
//            ideFrame {
//                projectView {
//                    // Navigate to a specific class in Android project (MainActivity)
//                    projectViewTree.expandPath(
//                        "MyApplication2",
//                        "app",
//                        "src",
//                        "main",
//                        "java",
//                        "com.example.myapplication2",
//                        "MainActivity"
//                    )
//                    projectViewTree.rightClickPath(
//                        "MyApplication2",
//                        "app",
//                        "src",
//                        "main",
//                        "java",
//                        "com.example.myapplication2",
//                        "MainActivity"
//                    )
//                }
//
//                // Verify that the menu option is present
//                val menuOption = x(xQuery { byAccessibleName(MENU_OPTION_TEXT) })
//                menuOption.shouldBe(
//                    "'$MENU_OPTION_TEXT' menu option should be present in Android Studio on class",
//                    present
//                )
//            }
//        }
//    }
}


