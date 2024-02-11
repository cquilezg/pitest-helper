package com.cquilez.pitesthelper.actions

import com.cquilez.pitesthelper.services.ClassService
import com.cquilez.pitesthelper.services.ServiceProvider
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.packageDependencies.ui.DirectoryNode
import com.intellij.psi.PsiFile
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class RunMutationCoverageActionTest {

    @MockK
    lateinit var anActionEvent : AnActionEvent

    @MockK
    lateinit var project : Project

    @MockK
    lateinit var serviceProvider: ServiceProvider

    @MockK
    lateinit var classService: ClassService

    @MockK
    lateinit var psiFile: PsiFile

    @MockK
    lateinit var directoryNode: DirectoryNode

    @MockK
    lateinit var presentation: Presentation

    @InjectMockKs
    lateinit var action: RunMutationCoverageAction

    @Nested
    @DisplayName("Method: update(...)")
    inner class UpdateTest {

        @Test
        fun `PsiFile is present, action is visible`() {
            every { anActionEvent.project } returns project
            every { project.service<ServiceProvider>() } returns serviceProvider
            every { serviceProvider.mockedServiceMap[ClassService::class] } returns classService
            every { anActionEvent.getData(CommonDataKeys.PSI_FILE) } returns psiFile
            every { classService.isCodeFile(psiFile) } returns true
            every { anActionEvent.presentation } returns presentation
            every { presentation.isEnabledAndVisible = true } answers {}

            action.update(anActionEvent)

            verify { presentation.isEnabledAndVisible = true }
        }

        @Test
        fun `Array is present, action is visible`() {
            every { anActionEvent.project } returns project
            every { project.service<ServiceProvider>() } returns serviceProvider
            every { serviceProvider.mockedServiceMap[ClassService::class] } returns classService
            every { anActionEvent.getData(CommonDataKeys.PSI_FILE) } returns null
            every { anActionEvent.getData(CommonDataKeys.NAVIGATABLE_ARRAY) } returns arrayOf(directoryNode)
            every { anActionEvent.presentation } returns presentation
            every { presentation.isEnabledAndVisible = true } answers {}

            action.update(anActionEvent)

            verify { presentation.isEnabledAndVisible = true }
        }

        @Test
        fun `Neither array nor class are present, action is not visible`() {
            every { anActionEvent.project } returns project
            every { project.service<ServiceProvider>() } returns serviceProvider
            every { serviceProvider.mockedServiceMap[ClassService::class] } returns classService
            every { anActionEvent.getData(CommonDataKeys.PSI_FILE) } returns null
            every { anActionEvent.getData(CommonDataKeys.NAVIGATABLE_ARRAY) } returns null
            every { anActionEvent.presentation } returns presentation
            every { presentation.isEnabledAndVisible = false } answers {}

            action.update(anActionEvent)

            verify { presentation.isEnabledAndVisible = false }
        }
    }


}