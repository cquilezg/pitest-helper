package com.cquilez.pitesthelper.mutationcoverage

import com.cquilez.pitesthelper.actions.RunMutationCoverageAction
import com.cquilez.pitesthelper.common.TestUtil.Companion.createDataContext
import com.cquilez.pitesthelper.common.TestUtil.Companion.createFixtureBuilder
import com.cquilez.pitesthelper.common.TestUtil.Companion.findAndCreateDirectoryTreeNode
import com.cquilez.pitesthelper.common.TestUtil.Companion.loadTestProject
import com.cquilez.pitesthelper.common.TestUtil.Companion.newClassTreeNode
import com.cquilez.pitesthelper.model.MutationCoverageData
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Tests Run mutation coverage action when a multiple nodes is selected
 */
class MutationCoverageSpecialCasesTest {

    companion object {

        private lateinit var fixture: JavaCodeInsightTestFixture

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            val projectBuilder = createFixtureBuilder()
            fixture = loadTestProject(projectBuilder, "sampleProject")
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            fixture.tearDown()
        }
    }

    @Test
    fun `Main Class selected, multiple test class candidates and one in same package, select test class in same package`() {
        val classA = fixture.configureFromTempProjectFile("src/main/java/com/myproject/package1/ClassA.java")
        ApplicationManager.getApplication().runReadAction {
            val dataContext = createDataContext(fixture, arrayOf(newClassTreeNode(fixture.project, classA)))
            val event = TestActionEvent(dataContext)
            val action = RunMutationCoverageAction()

            action.update(event)
            assertTrue(event.presentation.isVisible)

            action.actionPerformed(event)

            val data = RunMutationCoverageAction.mutationCoverageData as MutationCoverageData
            assertThat(data.targetClasses, Matchers.contains("com.myproject.package1.ClassA"))
            assertThat(data.targetTests, Matchers.contains("com.myproject.package1.ClassATest"))
        }
    }

    @Test
    fun `Main Class selected, multiple test class candidates and one in a superior package, select test class in superior package`() {
        val classA = fixture.configureFromTempProjectFile("src/main/java/com/myproject/package3/impl/ClassC.java")
        ApplicationManager.getApplication().runReadAction {
            val dataContext = createDataContext(fixture, arrayOf(newClassTreeNode(fixture.project, classA)))
            val event = TestActionEvent(dataContext)
            val action = RunMutationCoverageAction()

            action.update(event)
            assertTrue(event.presentation.isVisible)

            action.actionPerformed(event)

            val data = RunMutationCoverageAction.mutationCoverageData as MutationCoverageData
            assertThat(data.targetClasses, Matchers.contains("com.myproject.package3.impl.ClassC"))
            assertThat(data.targetTests, Matchers.contains("com.myproject.package3.ClassCTest"))
        }
    }
}