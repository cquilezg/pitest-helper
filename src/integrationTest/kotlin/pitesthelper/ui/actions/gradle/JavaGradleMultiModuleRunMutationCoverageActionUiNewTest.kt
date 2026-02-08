package com.cquilez.pitesthelper.ui.actions.gradle

import com.cquilez.pitesthelper.ui.IDEInstance
import com.cquilez.pitesthelper.ui.UiTestExtension
import com.cquilez.pitesthelper.ui.actions.CommonUITestsNew
import com.intellij.ide.starter.driver.engine.BackgroundRun
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(UiTestExtension::class)
@Tag("ui")
@Tag("gradle")
@Tag("java")
class JavaGradleMultiModuleRunMutationCoverageActionUiNewTest {

    @IDEInstance(testName = "javaGradleMultiModuleUiTest", projectPath = PROJECT_NAME)
    private lateinit var run: BackgroundRun

    companion object {
        private const val PROJECT_NAME = "gradle-multi-module"
        private const val LANGUAGE = "java"

        @JvmStatic
        fun singleNodeTestCases() = CommonUITestsNew.gradleMultiModuleSingleNodeTestCases(LANGUAGE)

        @JvmStatic
        fun multiNodeTestCases() = CommonUITestsNew.gradleMultiModuleMultiNodeTestCases(LANGUAGE)
    }

    @Nested
    inner class SingleNodeSelectionTest {

        @AfterEach
        fun afterEachHook() = CommonUITestsNew.closeMutationCoverageDialogIfOpen(run)

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.cquilez.pitesthelper.ui.actions.gradle.JavaGradleMultiModuleRunMutationCoverageActionUiNewTest#singleNodeTestCases")
        fun testSingleNodeSelection(
            testName: String,
            nodePath: Array<String>,
            expectedTargetClasses: String,
            expectedTargetTests: String,
            expectedCommand: String
        ) = CommonUITestsNew.executeSingleNodeTest(
            run,
            nodePath,
            expectedTargetClasses,
            expectedTargetTests,
            expectedCommand
        )
    }

    @Nested
    inner class MultiNodeSelectionTest {

        @AfterEach
        fun afterEachHook() = CommonUITestsNew.closeMutationCoverageDialogIfOpen(run)

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.cquilez.pitesthelper.ui.actions.gradle.JavaGradleMultiModuleRunMutationCoverageActionUiNewTest#multiNodeTestCases")
        fun testMultiNodeSelection(
            testName: String,
            nodePaths: List<Array<String>>,
            expectedTargetClasses: String,
            expectedTargetTests: String,
            expectedCommand: String
        ) = CommonUITestsNew.executeMultiNodeTest(
            run,
            nodePaths,
            expectedTargetClasses,
            expectedTargetTests,
            expectedCommand
        )
    }
}
