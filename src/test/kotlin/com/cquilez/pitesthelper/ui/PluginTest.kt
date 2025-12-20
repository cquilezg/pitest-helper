package com.cquilez.pitesthelper.ui

//import com.cquilez.pitesthelper.ui.actions.CommonUITests.Companion.buildNodeList
import com.cquilez.pitesthelper.ui.actions.CommonUITests
import com.cquilez.pitesthelper.ui.steps.SharedSteps
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.jBlist
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.present
import com.intellij.driver.sdk.ui.shouldBe
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.NoCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.GitHubProject
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.project.NoProject
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

class PluginTest {
    init {
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

    @Test
    fun simpleTestWithoutProject() {
        Starter.newContext(
            testName = "testExample",
            TestCase(IdeProductProvider.IC, projectInfo = NoProject).withVersion("2025.1")
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
        }
    }

    @Test
    fun simpleTest() {
        Starter.newContext(
            "testExample",
            TestCase(
                IdeProductProvider.IC,
                GitHubProject.fromGithub(branchName = "master", repoRelativeUrl = "JetBrains/ij-perf-report-aggregator")
            ).withVersion("2025.1")
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
            waitForIndicators(1.minutes)
            ideFrame {
                x(xQuery { byVisibleText("Current File") }).click()
                val configurations = popup().jBlist(xQuery { contains(byVisibleText("Edit Configurations")) })
                configurations.shouldBe("Configuration list is not present", present)
                Assertions.assertTrue(
                    configurations.rawItems.contains("backup-data"),
                    "Configurations list doesn't contain 'backup-data' item: ${configurations.rawItems}"
                )
            }
        }
    }

    private val TEST_PROJECT = "sample-maven"
    private val BUILD_COMMAND = "mvn test-compile pitest:mutationCoverage"
    private val TARGET_CLASSES = "-DtargetClasses"
    private val TARGET_TESTS = "-DtargetTests"

    @Test
    fun sampleMavenTest() {
        Starter.newContext(
            "testExample",
            TestCase(
                IdeProductProvider.IC,
                LocalProjectInfo(Path("src/test/testData/sample-maven"))
            ).withVersion("2025.1")
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }.runIdeWithDriver().useDriverAndCloseIde {
//            Thread.sleep(30.minutes.inWholeMilliseconds)
            waitForIndicators(1.minutes)
            ideFrame {
//                x(xQuery { byVisibleText("Current File") }).click()
                SharedSteps.runMutationCoverageNew(
                    TEST_PROJECT,
                    this,
                    CommonUITests.buildNodeList(
                        null,
                        mutableListOf("src", "main", "java", "com.myproject", "package1", "ClassA")
                    ),
                    "$BUILD_COMMAND " +
                            "$TARGET_CLASSES=com.myproject.package1.ClassA " +
                            "$TARGET_TESTS=com.myproject.package1.ClassATest",
                    false
                )
            }
        }
    }
}