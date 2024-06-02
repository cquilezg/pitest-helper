package com.cquilez.pitesthelper.ui.actions.maven

import com.automation.remarks.junit5.Video
import com.cquilez.pitesthelper.ui.steps.SharedSteps
import com.cquilez.pitesthelper.ui.utils.StepsLogger
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.utils.waitForIgnoringError
import com.cquilez.pitesthelper.ui.utils.RemoteRobotExtension
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
        private const val TEST_PROJECT = "sampleProject"

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

    @Test
    @Video
    fun runMutationCoverage(remoteRobot: RemoteRobot) = with(remoteRobot) {
        SharedSteps.runMutationCoverage(
            TEST_PROJECT, remoteRobot, listOf("src", "main", "java", "com.myproject", "package1"), "ClassA",
            "mvn test-compile pitest:mutationCoverage -DtargetClasses=com.myproject.package1.ClassA -DtargetTests=com.myproject.package1.ClassATest",
            false
        )
    }
}