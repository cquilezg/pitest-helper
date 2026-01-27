package com.cquilez.pitesthelper.infrastructure.adapter.psi

import com.cquilez.pitesthelper.domain.CodeClass
import com.cquilez.pitesthelper.domain.CodePackage
import com.cquilez.pitesthelper.domain.CodeType
import com.cquilez.pitesthelper.domain.SourceFolder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiPackage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class JavaPsiAdapterTest {

    private lateinit var javaPsiAdapter: JavaPsiAdapter
    private lateinit var sourceFolder: SourceFolder

    @BeforeEach
    fun setUp() {
        javaPsiAdapter = JavaPsiAdapter()
        sourceFolder = SourceFolder(
            path = Paths.get("/project/src/main/java"),
            codeType = CodeType.PRODUCTION
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    @DisplayName("PsiJavaFile cases")
    inner class PsiJavaFileTests {

        @Test
        fun `returns CodeClass when PsiJavaFile has class with qualified name`() {
            // Arrange
            val filePath = "/project/src/main/java/com/example/MyClass.java"
            val qualifiedName = "com.example.MyClass"
            val simpleName = "MyClass"

            val virtualFile = mockk<VirtualFile> {
                every { path } returns filePath
            }
            val psiClass = mockk<PsiClass> {
                every { this@mockk.qualifiedName } returns qualifiedName
                every { name } returns simpleName
            }
            val psiJavaFile = mockk<PsiJavaFile> {
                every { this@mockk.virtualFile } returns virtualFile
                every { classes } returns arrayOf(psiClass)
            }

            // Act
            val result = javaPsiAdapter.getCodeElement(psiJavaFile, sourceFolder)

            // Assert
            assertIs<CodeClass>(result)
            assertEquals(Paths.get(filePath), result.path)
            assertEquals(qualifiedName, result.qualifiedName)
            assertEquals(simpleName, result.simpleName)
            assertEquals(sourceFolder, result.sourceFolder)
        }

        @Test
        fun `returns null when PsiJavaFile has class without qualified name`() {
            // Arrange
            val filePath = "/project/src/main/java/com/example/MyClass.java"

            val virtualFile = mockk<VirtualFile> {
                every { path } returns filePath
            }
            val psiClass = mockk<PsiClass> {
                every { qualifiedName } returns null
                every { name } returns "MyClass"
            }
            val psiJavaFile = mockk<PsiJavaFile> {
                every { this@mockk.virtualFile } returns virtualFile
                every { classes } returns arrayOf(psiClass)
            }

            // Act
            val result = javaPsiAdapter.getCodeElement(psiJavaFile, sourceFolder)

            // Assert
            assertNull(result)
        }

        @Test
        fun `returns null when PsiJavaFile has no classes`() {
            // Arrange
            val filePath = "/project/src/main/java/com/example/Empty.java"

            val virtualFile = mockk<VirtualFile> {
                every { path } returns filePath
            }
            val psiJavaFile = mockk<PsiJavaFile> {
                every { this@mockk.virtualFile } returns virtualFile
                every { classes } returns emptyArray()
            }

            // Act
            val result = javaPsiAdapter.getCodeElement(psiJavaFile, sourceFolder)

            // Assert
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("PsiDirectory cases")
    inner class PsiDirectoryTests {

        @Test
        fun `returns CodePackage when directory has package with qualified name`() {
            // Arrange
            val directoryPath = "/project/src/main/java/com/example"
            val qualifiedName = "com.example"

            val virtualFile = mockk<VirtualFile> {
                every { path } returns directoryPath
            }
            val psiPackage = mockk<PsiPackage> {
                every { this@mockk.qualifiedName } returns qualifiedName
            }
            val javaDirectoryService = mockk<JavaDirectoryService> {
                every { getPackage(any()) } returns psiPackage
            }
            val psiDirectory = mockk<PsiDirectory> {
                every { this@mockk.virtualFile } returns virtualFile
            }

            mockkStatic(JavaDirectoryService::class)
            every { JavaDirectoryService.getInstance() } returns javaDirectoryService

            // Act
            val result = javaPsiAdapter.getCodeElement(psiDirectory, sourceFolder)

            // Assert
            assertIs<CodePackage>(result)
            assertEquals(Paths.get(directoryPath), result.path)
            assertEquals(qualifiedName, result.qualifiedName)
            assertEquals(sourceFolder, result.sourceFolder)
        }

        @Test
        fun `returns CodePackage when qualified name is blank but package name exists`() {
            // Arrange
            val directoryPath = "/project/src/main/java/example"
            val packageName = "example"

            val virtualFile = mockk<VirtualFile> {
                every { path } returns directoryPath
            }
            val psiPackage = mockk<PsiPackage> {
                every { qualifiedName } returns ""
                every { name } returns packageName
            }
            val javaDirectoryService = mockk<JavaDirectoryService> {
                every { getPackage(any()) } returns psiPackage
            }
            val psiDirectory = mockk<PsiDirectory> {
                every { this@mockk.virtualFile } returns virtualFile
            }

            mockkStatic(JavaDirectoryService::class)
            every { JavaDirectoryService.getInstance() } returns javaDirectoryService

            // Act
            val result = javaPsiAdapter.getCodeElement(psiDirectory, sourceFolder)

            // Assert
            assertIs<CodePackage>(result)
            assertEquals(Paths.get(directoryPath), result.path)
            assertEquals(packageName, result.qualifiedName)
            assertEquals(sourceFolder, result.sourceFolder)
        }

        @Test
        fun `returns null when both qualified name and package name are blank`() {
            // Arrange
            val directoryPath = "/project/src/main/java"

            val virtualFile = mockk<VirtualFile> {
                every { path } returns directoryPath
            }
            val psiPackage = mockk<PsiPackage> {
                every { qualifiedName } returns ""
                every { name } returns ""
            }
            val javaDirectoryService = mockk<JavaDirectoryService> {
                every { getPackage(any()) } returns psiPackage
            }
            val psiDirectory = mockk<PsiDirectory> {
                every { this@mockk.virtualFile } returns virtualFile
            }

            mockkStatic(JavaDirectoryService::class)
            every { JavaDirectoryService.getInstance() } returns javaDirectoryService

            // Act
            val result = javaPsiAdapter.getCodeElement(psiDirectory, sourceFolder)

            // Assert
            assertNull(result)
        }

        @Test
        fun `returns null when package name is null`() {
            // Arrange
            val directoryPath = "/project/src/main/java"

            val virtualFile = mockk<VirtualFile> {
                every { path } returns directoryPath
            }
            val psiPackage = mockk<PsiPackage> {
                every { qualifiedName } returns ""
                every { name } returns null
            }
            val javaDirectoryService = mockk<JavaDirectoryService> {
                every { getPackage(any()) } returns psiPackage
            }
            val psiDirectory = mockk<PsiDirectory> {
                every { this@mockk.virtualFile } returns virtualFile
            }

            mockkStatic(JavaDirectoryService::class)
            every { JavaDirectoryService.getInstance() } returns javaDirectoryService

            // Act
            val result = javaPsiAdapter.getCodeElement(psiDirectory, sourceFolder)

            // Assert
            assertNull(result)
        }

        @Test
        fun `returns null when JavaDirectoryService returns null package`() {
            // Arrange
            val directoryPath = "/project/src/main/resources"

            val virtualFile = mockk<VirtualFile> {
                every { path } returns directoryPath
            }
            val javaDirectoryService = mockk<JavaDirectoryService> {
                every { getPackage(any()) } returns null
            }
            val psiDirectory = mockk<PsiDirectory> {
                every { this@mockk.virtualFile } returns virtualFile
            }

            mockkStatic(JavaDirectoryService::class)
            every { JavaDirectoryService.getInstance() } returns javaDirectoryService

            // Act
            val result = javaPsiAdapter.getCodeElement(psiDirectory, sourceFolder)

            // Assert
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("Unsupported PsiFileSystemItem cases")
    inner class UnsupportedPsiFileSystemItemTests {

        @Test
        fun `returns null for unsupported PsiFileSystemItem types`() {
            // Arrange
            val filePath = "/project/src/main/resources/config.xml"

            val virtualFile = mockk<VirtualFile> {
                every { path } returns filePath
            }
            // Using a generic PsiFileSystemItem that is neither PsiJavaFile nor PsiDirectory
            val psiFileSystemItem = mockk<PsiFileSystemItem> {
                every { this@mockk.virtualFile } returns virtualFile
            }

            // Act
            val result = javaPsiAdapter.getCodeElement(psiFileSystemItem, sourceFolder)

            // Assert
            assertNull(result)
        }
    }
}
