package com.cquilez.pitesthelper.ui

import com.cquilez.pitesthelper.ui.actions.CommonUITestsNew
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.ci.NoCIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import java.lang.reflect.Field
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.test.junit5.JUnit5Asserter.fail
import kotlin.time.Duration.Companion.minutes

class UiTestExtension : BeforeAllCallback, BeforeEachCallback, TestExecutionExceptionHandler, AfterAllCallback {

    companion object {
        private const val IDE_INSTANCE_KEY = "ideInstance"
        private const val INJECTED_KEY = "injected"
        private const val ROOT_TEST_CLASS_KEY = "rootTestClass"
    }

    override fun beforeAll(context: ExtensionContext) {
        val store = getStore(context)

        // Skip if IDE is already created (for nested classes)
        if (store.get(IDE_INSTANCE_KEY) != null) {
            return
        }

        val testClass = context.requiredTestClass

        // Find the field annotated with @IDEInstance
        val ideInstanceField = findIDEInstanceField(testClass)
            ?: throw IllegalStateException("No field annotated with @IDEInstance found in ${testClass.name}")

        val annotation = ideInstanceField.getAnnotation(IDEInstance::class.java)

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
                        fail("$testName fails: $message. \n$details")
                    }
                }
            }
        }

        // Create and start the IDE
        val run = newIDE(annotation.testName, annotation.projectPath).runIdeWithDriver()
        run.driver.withContext {
            waitForIndicators(10.minutes)
        }

        // Store in extension context for beforeEach injection and afterAll cleanup
        store.put(IDE_INSTANCE_KEY, run)
        // Store the root test class to know when to close the IDE
        store.put(ROOT_TEST_CLASS_KEY, testClass)
    }

    @Throws(IllegalAccessException::class)
    override fun beforeEach(context: ExtensionContext) {
        val store = getStore(context)

        // Only inject once (on the first test)
        if (store.get(INJECTED_KEY) == true) {
            return
        }

        val testClass = context.requiredTestClass
        val testInstance = context.requiredTestInstance

        // Find the field annotated with @IDEInstance (may be in enclosing class)
        val (ideInstanceField, fieldOwnerClass) = findIDEInstanceFieldWithOwner(testClass)
            ?: throw IllegalStateException("No field annotated with @IDEInstance found in ${testClass.name} or its enclosing classes")

        val run = store.get(IDE_INSTANCE_KEY, BackgroundRun::class.java)
            ?: throw IllegalStateException("IDE instance not found in store. Was beforeAll called?")

        // Get the correct instance to inject into (may be enclosing instance for inner classes)
        val targetInstance = getTargetInstance(testInstance, testClass, fieldOwnerClass)

        ideInstanceField.isAccessible = true
        ideInstanceField.set(targetInstance, run)

        store.put(INJECTED_KEY, true)
    }

    @Throws(Throwable::class)
    override fun handleTestExecutionException(context: ExtensionContext?, throwable: Throwable) {
        if (context != null) {
            takeScreenshotOnFailure(context)
        }
        throw throwable
    }

    private fun takeScreenshotOnFailure(context: ExtensionContext) {
        try {
            val store = getStore(context)
            val run = store.get(IDE_INSTANCE_KEY, BackgroundRun::class.java) ?: return

            val testName = context.displayName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val screenshotDir = Path("build/test-screenshots")
            Files.createDirectories(screenshotDir)

            val screenshotPath = screenshotDir.resolve("${testName}_$timestamp.png")

            run.driver.takeScreenshot(screenshotPath.toString())

            println("Screenshot saved: $screenshotPath")
        } catch (e: Exception) {
            println("Failed to take screenshot: ${e.message}")
        }
    }

    override fun afterAll(context: ExtensionContext) {
        val store = getStore(context)
        val rootTestClass = store.get(ROOT_TEST_CLASS_KEY, Class::class.java)

        // Only close the IDE when afterAll is called for the root test class (not nested classes)
        if (context.requiredTestClass == rootTestClass) {
            val run = store.get(IDE_INSTANCE_KEY, BackgroundRun::class.java)
            if (run != null) {
                CommonUITestsNew.clearProjectNodeCache(run)
                run.closeIdeAndWait()
            }
        }
    }

    private fun newIDE(testName: String, projectPath: String): IDETestContext =
        Starter.newContext(
            testName = testName,
            TestCase(
                IdeProductProvider.IC,
                LocalProjectInfo(Path("src/integrationTest/testData/$projectPath"))
            ).withVersion(CommonUITestsNew.INTELLIJ_VERSION)
        ).apply {
            val pathToPlugin = System.getProperty("path.to.build.plugin")
            PluginConfigurator(this).installPluginFromPath(Path(pathToPlugin))
        }

    /**
     * Finds the field annotated with @IDEInstance in the given class or its enclosing classes.
     * Returns both the field and the class that owns it.
     */
    private fun findIDEInstanceFieldWithOwner(testClass: Class<*>): Pair<Field, Class<*>>? {
        var currentClass: Class<*>? = testClass
        while (currentClass != null) {
            val field = currentClass.declaredFields.find { field ->
                field.isAnnotationPresent(IDEInstance::class.java)
            }
            if (field != null) {
                return Pair(field, currentClass)
            }
            // Move to enclosing class (for inner classes)
            currentClass = currentClass.declaringClass
        }
        return null
    }

    /**
     * Finds the field annotated with @IDEInstance in the given class or its enclosing classes.
     */
    private fun findIDEInstanceField(testClass: Class<*>): Field? {
        return findIDEInstanceFieldWithOwner(testClass)?.first
    }

    /**
     * Gets the target instance where the field should be injected.
     * For inner classes, this navigates to the enclosing instance.
     */
    private fun getTargetInstance(testInstance: Any, testClass: Class<*>, fieldOwnerClass: Class<*>): Any {
        if (testClass == fieldOwnerClass) {
            return testInstance
        }

        // Navigate through enclosing instances to find the owner
        var currentInstance = testInstance
        var currentClass: Class<*> = testClass

        while (currentClass != fieldOwnerClass) {
            // Inner classes have a synthetic field "this$0" referencing the enclosing instance
            val enclosingField = currentClass.declaredFields.find { it.name.startsWith("this$") }
                ?: throw IllegalStateException("Cannot find enclosing instance field in ${currentClass.name}")

            enclosingField.isAccessible = true
            currentInstance = enclosingField.get(currentInstance)
            currentClass = currentClass.declaringClass
                ?: throw IllegalStateException("No declaring class for ${currentClass.name}")
        }

        return currentInstance
    }

    private fun getStore(context: ExtensionContext): ExtensionContext.Store {
        var rootContext = context
        while (rootContext.parent.isPresent && rootContext.parent.get().testClass.isPresent) {
            rootContext = rootContext.parent.get()
        }
        return rootContext.getStore(ExtensionContext.Namespace.create(UiTestExtension::class.java))
    }
}
