package com.cquilez.pitesthelper.ui.actions.maven

import com.automation.remarks.junit5.Video
import com.cquilez.pitesthelper.ui.actions.CommonUITests
import com.cquilez.pitesthelper.ui.pages.idea
import com.cquilez.pitesthelper.ui.pages.mutationCoverageDialog
import com.cquilez.pitesthelper.ui.steps.SharedSteps
import com.cquilez.pitesthelper.ui.utils.RemoteRobotExtension
import com.cquilez.pitesthelper.ui.utils.StepsLogger
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.waitForIgnoringError
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration.ofMinutes

@ExtendWith(RemoteRobotExtension::class)
@Tag("ui")
@Tag("maven")
class MavenRunMutationCoverageActionUiTest {

    init {
        StepsLogger.init()
    }

    companion object {
        private const val TEST_PROJECT = "sample-maven"
        private const val BUILD_COMMAND = "mvn test-compile pitest:mutationCoverage"
        private const val TARGET_CLASSES = "-DtargetClasses"
        private const val TARGET_TESTS = "-DtargetTests"

        @JvmStatic
        @BeforeAll
        fun openProjectBeforeTests(remoteRobot: RemoteRobot) {
            SharedSteps.openProjectBeforeTests(remoteRobot, TEST_PROJECT)
        }

        @JvmStatic
        @AfterAll
        fun closeProjectAfterTests(remoteRobot: RemoteRobot) {
            SharedSteps.closeProjectAfterTests(remoteRobot)
        }
    }

    @BeforeEach
    fun waitForIde(remoteRobot: RemoteRobot) {
        waitForIgnoringError(ofMinutes(3)) { remoteRobot.callJs("true") }
    }

    @AfterEach
    fun closeMutationCoverageDialogIfOpened(remoteRobot: RemoteRobot) {
        with(remoteRobot) {
            idea {
                if (isMutationCoverageOpen()) {
                    mutationCoverageDialog {
                        cancelButton.click()
                    }
                }
            }
        }
    }

    @Tag("single-node")
    @Nested
    @DisplayName("Single node tests")
    inner class SingleNodeTest {
        @Test
        @Video
        fun `Single Main Class selected and Test Class exists, single target class and single test class`(remoteRobot: RemoteRobot) =
            CommonUITests.SingleNodeTest.singleMainClass_testClassExists_singleMainClassAndSingleTestClass(
                TEST_PROJECT,
                BUILD_COMMAND,
                TARGET_CLASSES,
                TARGET_TESTS,
                remoteRobot
            )

        @Test
        @Video
        fun `Single Main Package selected and Test Package exists, single target package and single test package`(
            remoteRobot: RemoteRobot
        ) = CommonUITests.SingleNodeTest.singleMainPackage_testPackageExists_singleMainPackageAndSingleTestPackage(
            TEST_PROJECT,
            BUILD_COMMAND,
            TARGET_CLASSES,
            TARGET_TESTS,
            remoteRobot
        )

//        @Test
//        fun testNodeTree_simpleMaven(remoteRobot: RemoteRobot) {
//            with(remoteRobot) {
//                idea {
//                    projectToolWindow {
//                        val projectTree = SharedSteps.buildProjectTree(projectViewTree.data.getAll())
//                        val sampleMavenNode = SharedSteps.findNodes(projectTree.nodes[0], listOf("sample-maven"))
//                        assertThat(sampleMavenNode!!.remoteTexts[0].text, equalTo("sample-maven"))
//                    }
//                }
//            }
//        }
//
//        @Test
//        fun testNodeTree_src(remoteRobot: RemoteRobot) {
//            with(remoteRobot) {
//                idea {
//                    projectToolWindow {
//                        val projectTree = SharedSteps.buildProjectTree(projectViewTree.data.getAll())
//                        val srcNode = SharedSteps.findNodes(projectTree.nodes[0], listOf("sample-maven", "src"))
//                        assertThat(srcNode!!.remoteTexts[0].text, equalTo("src"))
//                    }
//                }
//            }
//        }
//
//        @Test
//        fun testNodeTree_main_fail(remoteRobot: RemoteRobot) {
//            with(remoteRobot) {
//                idea {
//                    projectToolWindow {
//                        val projectTree = SharedSteps.buildProjectTree(projectViewTree.data.getAll())
//                        val mainNode = SharedSteps.findNodes(projectTree.nodes[0], listOf("sample-maven", "main"))
//                        assertNull(mainNode)
//                    }
//                }
//            }
//        }
//
//        @Test
//        fun testNodeTree_testPackage1(remoteRobot: RemoteRobot) {
//            with(remoteRobot) {
//                idea {
//                    projectToolWindow {
//                        val projectTree = SharedSteps.buildProjectTree(projectViewTree.data.getAll())
//                        val testPackage1 = SharedSteps.findNodes(projectTree.nodes[0], listOf("src", "test", "java", "com.myproject", "package1"))
//                        assertThat(testPackage1!!.remoteTexts[0].text, equalTo("package1"))
//                    }
//                }
//            }
//        }

        @Test
        @Video
        fun `Single Test Class selected and Main Class exists, single target class and single test class`(remoteRobot: RemoteRobot) =
            CommonUITests.SingleNodeTest.singleTestClass_mainClassExists_singleTargetClassAndSingleTestClass(
                TEST_PROJECT,
                BUILD_COMMAND,
                TARGET_CLASSES,
                TARGET_TESTS,
                remoteRobot
            )

        @Test
        @Video
        fun `Single Test Package selected and Main Package exists, single target package and single test package`(
            remoteRobot: RemoteRobot
        ) = CommonUITests.SingleNodeTest.singleTestPackage_mainPackageExists_singleTargetPackageAndSingleTestPackage(
            TEST_PROJECT,
            BUILD_COMMAND,
            TARGET_CLASSES,
            TARGET_TESTS,
            remoteRobot
        )
    }

    @Tag("multi-node")
    @Nested
    @DisplayName("Multi node tests")
    inner class MultiNodeTest {
        @Test
        @Video
        fun `Two Main Classes selected and Test Classes exists, two target classes and two test classes`(remoteRobot: RemoteRobot) =
            CommonUITests.MultiNodeTest.twoMainClassesSelected_testClassesExists_TwoTargetClassesAndTwoTestClasses(
                TEST_PROJECT,
                BUILD_COMMAND,
                TARGET_CLASSES,
                TARGET_TESTS,
                remoteRobot
            )

        @Test
        @Video
        fun `Two Test Classes selected and Main Classes exists, two target classes and two test classes`(remoteRobot: RemoteRobot) =
            CommonUITests.MultiNodeTest.twoTestClassesSelected_mainClassesExists_twoTargetClassesAndTwoTestClasses(
                TEST_PROJECT,
                BUILD_COMMAND,
                TARGET_CLASSES,
                TARGET_TESTS,
                remoteRobot
            )

        @Test
        @Video
        fun `Two Main Packages selected and Test Packages exists, two target packages and two test packages`(remoteRobot: RemoteRobot) =
            CommonUITests.MultiNodeTest.twoMainPackages_testPackagesExists_twoMainPackagesAndTwoTestPackages(
                TEST_PROJECT,
                BUILD_COMMAND,
                TARGET_CLASSES,
                TARGET_TESTS,
                remoteRobot
            )

        @Test
        @Video
        fun `Two Test Packages selected and Main Packages exists, two target packages and two test packages`(remoteRobot: RemoteRobot) =
            CommonUITests.MultiNodeTest.twoTestPackages_mainPackagesExists_TwoMainPackagesAndTwoTestPackages(
                TEST_PROJECT,
                BUILD_COMMAND,
                TARGET_CLASSES,
                TARGET_TESTS,
                remoteRobot
            )

        @Test
        @Video
        fun `Main Class and its package selected, test package exists, only packages are selected`(remoteRobot: RemoteRobot) =
            CommonUITests.MultiNodeTest.mainClassAndItsPackage_testPackageExists_onlyPackagesAreSelected(
                TEST_PROJECT,
                BUILD_COMMAND,
                TARGET_CLASSES,
                TARGET_TESTS,
                remoteRobot
            )

        @Test
        @Video
        fun `Test Class and its package selected, main package exists, only packages are selected`(remoteRobot: RemoteRobot) =
            CommonUITests.MultiNodeTest.testClassAndParentPackage_mainPackageExists_onlyPackageSelected(
                TEST_PROJECT,
                BUILD_COMMAND,
                TARGET_CLASSES,
                TARGET_TESTS,
                remoteRobot
            )
    }

    @Tag("cross-source")
    @Nested
    @DisplayName("Cross source tests")
    inner class CrossSourceTest {
        @Test
        @Video
        fun `Main Class and its test class selected, both classes are selected`(remoteRobot: RemoteRobot) =
            CommonUITests.CrossSourceTest.mainClassAndItsTestClass_bothClassesAreSelected(
                TEST_PROJECT,
                BUILD_COMMAND,
                TARGET_CLASSES,
                TARGET_TESTS,
                remoteRobot
            )

        @Test
        @Video
        fun `Main Class and different test class selected, main classes and test classes selected`(remoteRobot: RemoteRobot) =
            CommonUITests.CrossSourceTest.mainClassAndDifferentTestClass_mainClassesAndTestClassesSelected(
                TEST_PROJECT,
                BUILD_COMMAND,
                TARGET_CLASSES,
                TARGET_TESTS,
                remoteRobot
            )
    }

    @Tag("special-cases")
    @Nested
    @DisplayName("Special cases tests")
    inner class SpecialCasesTest {
        @Test
        @Video
        fun `Main Class selected, multiple test class candidates and one in same package, select test class in same package`(
            remoteRobot: RemoteRobot
        ) =
            CommonUITests.SpecialCasesTest.mainClass_multipleTestClassCandidatesAndOneInSamePackage_testClassInSamePackage(
                TEST_PROJECT,
                BUILD_COMMAND,
                TARGET_CLASSES,
                TARGET_TESTS,
                remoteRobot
            )

        @Test
        @Video
        fun `Main Class selected, multiple test class candidates and one in a superior package, select test class in superior package`(
            remoteRobot: RemoteRobot
        ) =
            CommonUITests.SpecialCasesTest.mainClass_multipleTestClassCandidatesAndOneInASuperiorPackage_testClassInSuperiorPackage(
                TEST_PROJECT,
                BUILD_COMMAND,
                TARGET_CLASSES,
                TARGET_TESTS,
                remoteRobot
            )

//        @Test
//        @Video
//        fun `Test Class selected, single main class in a superior package, select main class in superior package`(remoteRobot: RemoteRobot) =
//            CommonUITests.SpecialCasesTest.testClass_singleMainClassCandidateInASuperiorPackage_mainClassFound(
//                TEST_PROJECT,
//                BUILD_COMMAND,
//                TARGET_CLASSES,
//                TARGET_TESTS,
//                remoteRobot
//            )
    }


}