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
class MutationCoverageMultiNodeTest {

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
    fun `Two Main Classes selected and Test Classes exists, two target classes and two test classes`() {
        val classA = fixture.configureFromTempProjectFile("src/main/java/com/myproject/package1/ClassA.java")
        val classB = fixture.configureFromTempProjectFile("src/main/java/com/myproject/package2/ClassB.java")
        ApplicationManager.getApplication().runReadAction {
            val dataContext = createDataContext(fixture, arrayOf(newClassTreeNode(fixture.project, classA), newClassTreeNode(fixture.project, classB)))
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

    @Test
    fun `Two Test Classes selected and Main Classes exists, two target classes and two test classes`() {
        val classATest = fixture.configureFromTempProjectFile("src/test/java/com/myproject/package1/ClassATest.java")
        val classBTest = fixture.configureFromTempProjectFile("src/test/java/com/myproject/package2/ClassBTest.java")
        ApplicationManager.getApplication().runReadAction {
            val dataContext = createDataContext(fixture, arrayOf(newClassTreeNode(fixture.project, classATest), newClassTreeNode(fixture.project, classBTest)))
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

    @Test
    fun `Two Main Packages selected and Test Packages exists, two target packages and two test packages`() {
        ApplicationManager.getApplication().runReadAction {
            val package1 = findAndCreateDirectoryTreeNode(fixture, "src/main/java/com/myproject/package1")
            val package2 = findAndCreateDirectoryTreeNode(fixture, "src/main/java/com/myproject/package2")
            val dataContext = createDataContext(fixture, arrayOf(package1, package2))
            val event = TestActionEvent(dataContext)
            val action = RunMutationCoverageAction()

            action.update(event)
            assertTrue(event.presentation.isVisible)

            action.actionPerformed(event)

            val data = RunMutationCoverageAction.mutationCoverageData as MutationCoverageData
            assertThat(data.targetClasses, Matchers.contains("com.myproject.package1.*", "com.myproject.package2.*"))
            assertThat(data.targetTests, Matchers.contains("com.myproject.package1.*", "com.myproject.package2.*"))
        }
    }

    @Test
    fun `Two Test Packages selected and Main Packages exists, two target packages and two test packages`() {
        ApplicationManager.getApplication().runReadAction {
            val package1 = findAndCreateDirectoryTreeNode(fixture, "src/test/java/com/myproject/package1")
            val package2 = findAndCreateDirectoryTreeNode(fixture, "src/test/java/com/myproject/package2")
            val dataContext = createDataContext(fixture, arrayOf(package1, package2))
            val event = TestActionEvent(dataContext)
            val action = RunMutationCoverageAction()

            action.update(event)
            assertTrue(event.presentation.isVisible)

            action.actionPerformed(event)

            val data = RunMutationCoverageAction.mutationCoverageData as MutationCoverageData
            assertThat(data.targetClasses, Matchers.contains("com.myproject.package1.*", "com.myproject.package2.*"))
            assertThat(data.targetTests, Matchers.contains("com.myproject.package1.*", "com.myproject.package2.*"))
        }
    }

    @Test
    fun `Main Class and its package selected, test package exists, only packages are selected`() {
        val classA = fixture.configureFromTempProjectFile("src/main/java/com/myproject/package1/ClassA.java")
        ApplicationManager.getApplication().runReadAction {
            val package1 = findAndCreateDirectoryTreeNode(fixture, "src/main/java/com/myproject/package1")
            val dataContext = createDataContext(fixture, arrayOf(newClassTreeNode(fixture.project, classA), package1))
            val event = TestActionEvent(dataContext)
            val action = RunMutationCoverageAction()

            action.update(event)
            assertTrue(event.presentation.isVisible)

            action.actionPerformed(event)

            val data = RunMutationCoverageAction.mutationCoverageData as MutationCoverageData
            assertThat(data.targetClasses, Matchers.contains("com.myproject.package1.*"))
            assertThat(data.targetTests, Matchers.contains("com.myproject.package1.*"))
        }
    }

    @Test
    fun `Test Class and its package selected, main package exists, only packages are selected`() {
        val classA = fixture.configureFromTempProjectFile("src/test/java/com/myproject/package1/ClassATest.java")
        ApplicationManager.getApplication().runReadAction {
            val package1 = findAndCreateDirectoryTreeNode(fixture, "src/test/java/com/myproject/package1")
            val dataContext = createDataContext(fixture, arrayOf(newClassTreeNode(fixture.project, classA), package1))
            val event = TestActionEvent(dataContext)
            val action = RunMutationCoverageAction()

            action.update(event)
            assertTrue(event.presentation.isVisible)

            action.actionPerformed(event)

            val data = RunMutationCoverageAction.mutationCoverageData as MutationCoverageData
            assertThat(data.targetClasses, Matchers.contains("com.myproject.package1.*"))
            assertThat(data.targetTests, Matchers.contains("com.myproject.package1.*"))
        }
    }

    @Test
    fun `Main Class and different package selected, test class and package exists, main and test code selected`() {
        val classA = fixture.configureFromTempProjectFile("src/main/java/com/myproject/package1/ClassA.java")
        ApplicationManager.getApplication().runReadAction {
            val package1 = findAndCreateDirectoryTreeNode(fixture, "src/main/java/com/myproject/package2")
            val dataContext = createDataContext(fixture, arrayOf(newClassTreeNode(fixture.project, classA), package1))
            val event = TestActionEvent(dataContext)
            val action = RunMutationCoverageAction()

            action.update(event)
            assertTrue(event.presentation.isVisible)

            action.actionPerformed(event)

            val data = RunMutationCoverageAction.mutationCoverageData as MutationCoverageData
            assertThat(data.targetClasses, Matchers.contains("com.myproject.package1.ClassA", "com.myproject.package2.*"))
            assertThat(data.targetTests, Matchers.contains("com.myproject.package1.ClassATest", "com.myproject.package2.*"))
        }
    }

    @Test
    fun `Test Class and different package selected, main class and package exists, main and test code selected`() {
        val classATest = fixture.configureFromTempProjectFile("src/test/java/com/myproject/package1/ClassATest.java")
        ApplicationManager.getApplication().runReadAction {
            val package1 = findAndCreateDirectoryTreeNode(fixture, "src/test/java/com/myproject/package2")
            val dataContext = createDataContext(fixture, arrayOf(newClassTreeNode(fixture.project, classATest), package1))
            val event = TestActionEvent(dataContext)
            val action = RunMutationCoverageAction()

            action.update(event)
            assertTrue(event.presentation.isVisible)

            action.actionPerformed(event)

            val data = RunMutationCoverageAction.mutationCoverageData as MutationCoverageData
            assertThat(data.targetClasses, Matchers.contains("com.myproject.package1.ClassA", "com.myproject.package2.*"))
            assertThat(data.targetTests, Matchers.contains("com.myproject.package1.ClassATest", "com.myproject.package2.*"))
        }
    }
}