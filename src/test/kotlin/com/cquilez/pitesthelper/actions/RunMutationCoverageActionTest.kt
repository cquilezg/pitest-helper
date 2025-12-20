package com.cquilez.pitesthelper.actions

import com.cquilez.pitesthelper.infrastructure.action.RunMutationCoverageFromProjectViewAction
import com.cquilez.pitesthelper.infrastructure.services.BuildSystemService
import com.cquilez.pitesthelper.processors.MutationCoverageCommandProcessor
import com.cquilez.pitesthelper.services.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.packageDependencies.ui.DirectoryNode
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiFile
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class RunMutationCoverageFromProjectViewActionTest {

    @MockK
    lateinit var anActionEvent: AnActionEvent

    @MockK
    lateinit var project: Project

    @MockK
    lateinit var serviceProvider: ServiceProvider

    @MockK
    lateinit var classService: ClassService

    @MockK
    lateinit var projectService: MyProjectService

    @MockK
    lateinit var buildSystemService: BuildSystemService

    @MockK
    lateinit var uiService: UIService

    @MockK
    lateinit var languageProcessorService: LanguageProcessorService

    @MockK
    lateinit var psiFile: PsiFile

    @MockK
    lateinit var directoryNode: DirectoryNode

    @MockK
    lateinit var presentation: Presentation

    @MockK
    lateinit var module: Module

    @MockK
    lateinit var navigatable: Navigatable

    @MockK
    lateinit var commandProcessor: MutationCoverageCommandProcessor

    @InjectMockKs
    lateinit var action: RunMutationCoverageFromProjectViewAction

    @Nested
    @DisplayName("Method: update(...)")
    inner class UpdateTest {

        @Test
        fun `PsiFile is present, action is visible`() {
            every { anActionEvent.project } returns project
            every { project.service<ServiceProvider>() } returns serviceProvider
            every { serviceProvider.mockedServiceMap[MyProjectService::class] } returns projectService
            every { anActionEvent.getData(CommonDataKeys.PSI_FILE) } returns psiFile
            every { projectService.isSupportedPsiFile(psiFile) } returns true
            every { anActionEvent.presentation } returns presentation
            every { presentation.isEnabledAndVisible = true } answers {}

            action.update(anActionEvent)

            verify { presentation.isEnabledAndVisible = true }
        }

        @Test
        fun `Array is present, action is visible`() {
            every { anActionEvent.project } returns project
            every { project.service<ServiceProvider>() } returns serviceProvider
            every { serviceProvider.mockedServiceMap[MyProjectService::class] } returns projectService
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
            every { serviceProvider.mockedServiceMap[MyProjectService::class] } returns projectService
            every { anActionEvent.getData(CommonDataKeys.PSI_FILE) } returns null
            every { anActionEvent.getData(CommonDataKeys.NAVIGATABLE_ARRAY) } returns null
            every { anActionEvent.presentation } returns presentation
            every { presentation.isEnabledAndVisible = false } answers {}

            action.update(anActionEvent)

            verify { presentation.isEnabledAndVisible = false }
        }
    }

    @Nested
    @DisplayName("Method: actionPerformed(...)")
    inner class ActionPerformedTest {

//        @Test
//        fun `Invokes command processor and shows dialog with data`() {
//            every { anActionEvent.project } returns project
//            every { project.service<ServiceProvider>() } returns serviceProvider
//            every { serviceProvider.mockedServiceMap[MyProjectService::class] } returns projectService
//            every { serviceProvider.mockedServiceMap[BuildSystemService::class] } returns buildSystemService
//            every { serviceProvider.mockedServiceMap[UIService::class] } returns uiService
//            every { serviceProvider.mockedServiceMap[ClassService::class] } returns classService
//            every { serviceProvider.mockedServiceMap[LanguageProcessorService::class] } returns languageProcessorService
//            val navigatableArray = arrayOf(navigatable)
//            every { anActionEvent.getData(CommonDataKeys.NAVIGATABLE_ARRAY) } returns navigatableArray
//            every { anActionEvent.getData(CommonDataKeys.PSI_FILE) } returns psiFile
//            val mainList = Collections.emptyList<String>()
//            val testList = Collections.emptyList<String>()
//            val mutationCoverageData = MutationCoverageData(module, mainList, testList)
//            mockkConstructor(MutationCoverageCommandProcessor::class)
//            every {
//                buildSystemService.getCommandBuilder(project, projectService, classService, languageProcessorService, navigatableArray, psiFile)
//            } returns commandProcessor
//            every {
//                commandProcessor.handleCommand()
//            } answers { mutationCoverageData }
//
//            val showDialogRun = slot<Runnable>()
//            val showDialogRun2 = slot<Runnable>()
//            every { uiService.showDialog(capture(showDialogRun), capture(showDialogRun2)) } answers { }
//
//            action.actionPerformed(anActionEvent)
//
//            assertNotNull(showDialogRun.captured)
//            assertNotNull(showDialogRun2.captured)
//        }
    }
}