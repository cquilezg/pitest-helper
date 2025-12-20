package com.cquilez.pitesthelper.infrastructure.ui.adapter

import com.android.tools.idea.navigator.nodes.android.AndroidModuleNode
import com.cquilez.pitesthelper.application.port.out.CodeElementPort
import com.cquilez.pitesthelper.domain.model.CodePackage
import com.cquilez.pitesthelper.infrastructure.ui.NavigatablePort
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Tests the conversion from Android project view nodes to PSI elements.
 * 
 * This test simulates right-clicking on Android project nodes (like the "app" module folder)
 * and verifies that the conversion chain works correctly:
 * Navigatable → Path → PSI → CodeElement
 * 
 * Uses LightPlatformTestCase for fast, headless testing without UI overhead.
 */
class AndroidProjectViewToPsiConversionTest : LightPlatformTestCase() {

    private lateinit var navigatablePort: NavigatablePort
    private lateinit var codeElementPort: CodeElementPort

    @BeforeEach
    override fun setUp() {
        super.setUp()
        
        // Load Android project structure
        // Note: In a real scenario, you'd load the full Android project
        // For testing, we simulate the module structure
        
        // Get services
        navigatablePort = ApplicationManager.getApplication().service<NavigatablePort>()
        codeElementPort = project.service<CodeElementPort>()
    }

    /**
     * Test: Right-click on the "app" Android module folder
     * 
     * This simulates what happens when a user right-clicks on the "app" module
     * in the Android project view. The test verifies:
     * 1. AndroidModuleNode is correctly converted to a Path
     * 2. The Path is correctly converted to PSI elements
     * 3. The PSI elements are correctly converted to CodeElements
     */
    @Test
    fun testRightClickAndroidAppModule_ConvertsToPsi() {
        // Step 1: Get the Android module (simulating what happens when you right-click "app")
        val module = ModuleManager.getInstance(project).modules.find { it.name == "app" }
            ?: ModuleManager.getInstance(project).modules.firstOrNull()
        
        assertNotNull(module, "Should have at least one module in the project")

        // Step 2: Create AndroidModuleNode (this is what you get when right-clicking the module)
        // AndroidModuleNode represents the module in the project view
        val androidModuleNode = AndroidModuleNode(project, module!!)
        
        // Step 3: Simulate right-click by creating Navigatables array
        // (This is what AnActionEvent.getData(CommonDataKeys.NAVIGATABLE_ARRAY) returns)
        val navigatables = arrayOf<com.intellij.pom.Navigatable>(androidModuleNode)

        // Step 4: Convert Navigatables to Paths (via AndroidNavigatableAdapter)
        // This tests AndroidNavigatableAdapter.getAbsolutePath() method
        val paths = navigatablePort.getAbsolutePaths(navigatables)
        
        assertFalse(paths.isEmpty(), "Should convert AndroidModuleNode to at least one path")
        assertEquals(1, paths.size, "Should convert single AndroidModuleNode to single path")
        
        val modulePath = paths.first()
        assertNotNull(modulePath, "Module path should not be null")
        
        // Verify the path points to the module directory
        // AndroidModuleNode uses externalProjectPath which should point to the module's build.gradle location
        val moduleFile = module.moduleFile
        assertNotNull(moduleFile, "Module should have a module file")
        val expectedPath = moduleFile!!.parent.toNioPath()
        assertEquals(expectedPath, modulePath, "Path should point to module directory")

        // Step 5: Convert Paths to PSI elements (via CodeElementAdapter)
        // This tests the full conversion chain: Path → PSI → CodeElement
        val (codeElements, errors) = codeElementPort.getCodeElements(paths)
        
        // Verify no errors occurred
        assertTrue(errors.isEmpty(), "Should not have errors: ${errors.joinToString("\n")}")
        
        // Verify we got code elements
        assertFalse(codeElements.isEmpty(), "Should convert module path to code elements")
        
        // When right-clicking an Android module, we expect to get the base package
        // from the production source folder
        val codePackage = codeElements.firstOrNull() as? CodePackage
        assertNotNull(codePackage, "Should get a CodePackage for the module")
        
        // Verify the package is from the production source folder
        assertEquals(
            com.cquilez.pitesthelper.domain.model.CodeType.PRODUCTION,
            codePackage!!.sourceFolder.codeType,
            "Package should be from production source folder"
        )
    }

    /**
     * Helper method to verify the conversion chain works end-to-end
     */
    private fun verifyConversionChain(
        navigatables: Array<com.intellij.pom.Navigatable>,
        expectedPath: Path,
        description: String
    ) {
        // Navigatable → Path
        val paths = navigatablePort.getAbsolutePaths(navigatables)
        assertEquals(1, paths.size, "$description: Should convert to single path")
        assertEquals(expectedPath, paths.first(), "$description: Path should match expected")

        // Path → PSI → CodeElement
        val (codeElements, errors) = codeElementPort.getCodeElements(paths)
        assertTrue(errors.isEmpty(), "$description: Should not have errors: ${errors.joinToString("\n")}")
        assertFalse(codeElements.isEmpty(), "$description: Should get code elements")
    }
}

