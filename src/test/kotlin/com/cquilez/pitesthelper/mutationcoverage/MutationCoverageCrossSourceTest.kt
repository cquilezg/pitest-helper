package com.cquilez.pitesthelper.mutationcoverage

import com.cquilez.pitesthelper.actions.RunMutationCoverageAction
import com.cquilez.pitesthelper.common.TestUtil.Companion.createDataContext
import com.cquilez.pitesthelper.common.TestUtil.Companion.createFixtureBuilder
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
 * Tests Run mutation coverage action when the nodes are in different sources
 */
class MutationCoverageCrossSourceTest {

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
    fun `Main Class and its test class selected, both classes are selected`() {
        val classA = fixture.configureFromTempProjectFile("src/main/java/com/myproject/package1/ClassA.java")
        val classATest = fixture.configureFromTempProjectFile("src/test/java/com/myproject/package1/ClassATest.java")
        ApplicationManager.getApplication().runReadAction {
            val dataContext = createDataContext(fixture, arrayOf(newClassTreeNode(fixture.project, classA), newClassTreeNode(fixture.project, classATest)))
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
    fun `Main Class and different test class selected, main classes and test classes selected`() {
        val classA = fixture.configureFromTempProjectFile("src/main/java/com/myproject/package1/ClassA.java")
        val classBTest = fixture.configureFromTempProjectFile("src/test/java/com/myproject/package2/ClassBTest.java")
        ApplicationManager.getApplication().runReadAction {
            val dataContext = createDataContext(fixture, arrayOf(newClassTreeNode(fixture.project, classA), newClassTreeNode(fixture.project, classBTest)))
            val event = TestActionEvent(dataContext)
            val action = RunMutationCoverageAction()

            action.update(event)
            assertTrue(event.presentation.isVisible)

            action.actionPerformed(event)

            val data = RunMutationCoverageAction.mutationCoverageData as MutationCoverageData
            assertThat(data.targetClasses, Matchers.contains("com.myproject.package1.ClassA", "com.myproject.package2.ClassB"))
            assertThat(data.targetTests, Matchers.contains("com.myproject.package1.ClassATest", "com.myproject.package2.ClassBTest"))
        }
    }
}