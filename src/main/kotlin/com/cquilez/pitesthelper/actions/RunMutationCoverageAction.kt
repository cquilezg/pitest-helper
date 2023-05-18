package com.cquilez.pitesthelper.actions

import com.cquilez.pitesthelper.services.ClassService
import com.cquilez.pitesthelper.ui.MyPITestDialog
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalView
import java.io.IOException

class RunMutationCoverageAction : DumbAwareAction(){

    private val stringBuilder = StringBuilder()
    private var project: Project? = null

    override fun update(@NotNull event: AnActionEvent) {
        // Get required data keys
        project = event.project
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)
        var visible = false
        // TODO: Check if the directory is inside of a module and in a source root
        if (psiFile != null && ClassService.isCodeFile(psiFile)) {
            visible = true
        } else {
            val navigatableArray = event.getData(CommonDataKeys.NAVIGATABLE_ARRAY)
            if (!navigatableArray.isNullOrEmpty()) {
                visible = true
            }
        }

        event.presentation.isEnabledAndVisible = visible
    }

    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)

        val project = project as Project

        stringBuilder.clear()
        val targetClasses: String?
        val targetTests: String?
        val module: Module?
        val navigatableArray = event.getData(CommonDataKeys.NAVIGATABLE_ARRAY)
        if (!navigatableArray.isNullOrEmpty()) {
            module = checkElementsInSameModule(project, navigatableArray)
            targetClasses = extractTargetClassesByArray(project, navigatableArray)
            targetTests = extractTargetTestsByArray(project, navigatableArray)
        } else if (psiFile != null && ClassService.isCodeFile(psiFile)) {
            val psiClass = ClassService.getPublicClass(psiFile)
            targetClasses = extractTargetClassesByPsiClass(psiClass)
            targetTests = extractTargetTestsByPsiClass(psiClass)
            module = getModule(psiFile)
        } else {
            throw IllegalArgumentException("No elements found")
        }

//            val dialog = MyDialog()
        val dialog = MyPITestDialog(project, module)
        dialog.targetClasses = targetClasses
        dialog.targetTests = targetTests
        dialog.show()

        // Comando que se ejecutará en la consola
//            val command = "java -jar \"/c/nova-le-7.4.1-windows/tools/nova/com.bbva.enoa.generator-2.8.1.jar\" --skipTests --parserName swagger --specPath \"continuousintegrationapi.yml\" --flavourType server --flavourCategory spring.nova --outcomePath \"/c/projects/java/bbva/nova/batch/novaagent/generated/continuousintegration/1.0.0/server\" --novaArchitectureService"
//            runCommand(project, command)
    }

    private fun checkElementsInSameModule(project: Project, navigatableArray: Array<Navigatable>?): Module {
        if (navigatableArray.isNullOrEmpty()) {
            throw IllegalArgumentException("The are no elements to process")
        }
        var module: Module? = null
        navigatableArray.forEach {
            val newModule: Module? = when (it) {
                is ClassTreeNode -> {
                    ModuleUtilCore.findModuleForFile((it).virtualFile!!, project)
                }

                is PsiDirectoryNode -> {
                    ModuleUtilCore.findModuleForFile((it.value).virtualFile, project)
                }

                else -> {
                    throw IllegalArgumentException("The element is not supported")
                }
            }
            if (newModule == null) {
                throw IllegalArgumentException("Module not found for element: ${it}")
            } else if (module == null) {
                module = newModule
            } else if (module != newModule) {
                throw IllegalArgumentException("You cannot choose elements from different modules")
            }
        }
        if (module == null) {
            throw IllegalArgumentException("Module not found")
        }
        return module as Module
    }

    private fun extractTargetClassesByPsiClass(psiClass: PsiClass): String {
        val packageName = getPackageName(psiClass)
        if (psiClass.name != null) {
            val className = psiClass.name as String
            val classUnderTest: String = className.removeSuffix("Test")
            return "$packageName.${classUnderTest}"
        } else {
            throw IllegalArgumentException("The class name cannot be found")
        }
    }

    private fun extractTargetTestsByPsiClass(psiClass: PsiClass): String {
        val packageName = getPackageName(psiClass)
        if (psiClass.name != null) {
            return "$packageName.${psiClass.name}"
        } else {
            throw IllegalArgumentException("The test class name cannot be found")
        }
    }

    private fun extractTargetClassesByArray(project: Project, navigatableArray: Array<out Navigatable>?): String {
        val targetClassesList = mutableListOf<String>()
        val javaDirectoryService = JavaDirectoryService.getInstance()
        if (!navigatableArray.isNullOrEmpty()) {
            navigatableArray.forEach {
                if (it is PsiDirectoryNode) {
                    val javaPackage = javaDirectoryService.getPackage(it.value)
                    if (javaPackage != null) {
                        targetClassesList.add("${javaPackage.qualifiedName}.*")
                    } else {
                        throw IllegalArgumentException("The element is not a package: ${it.name}")
                    }
//                module = ModuleUtilCore.findModuleForFile(psiDirectory.virtualFile, project)
                } else if (it is ClassTreeNode) {
                    val psiManager = PsiManager.getInstance(project)
                    val virtualFile = it.virtualFile
                    if (virtualFile != null) {
                        val psiFile = psiManager.findFile(virtualFile)
                        if (psiFile is PsiJavaFile) {
                            targetClassesList.add("${psiFile.packageName}.${it.name}")
                        } else {
                            throw IllegalArgumentException("The file is not a Java file: ${it.name}")
                        }
                    } else {
                        throw IllegalArgumentException("Virtual File not found for ${it.name}")
                    }
                }
            }
        }
        return targetClassesList.joinToString(",")
    }

    private fun extractTargetTestsByArray(project: Project, navigatableArray: Array<out Navigatable>?): String {
        val targetClassesList = mutableListOf<String>()
        val javaDirectoryService = JavaDirectoryService.getInstance()
        if (!navigatableArray.isNullOrEmpty()) {
            navigatableArray.forEach {
                if (it is PsiDirectoryNode) {
                    val javaPackage = javaDirectoryService.getPackage(it.value)
                    if (javaPackage != null) {
                        targetClassesList.add("${javaPackage.qualifiedName}.*")
                    } else {
                        throw IllegalArgumentException("The element is not a package: ${it.name}")
                    }
//                module = ModuleUtilCore.findModuleForFile(psiDirectory.virtualFile, project)
                } else if (it is ClassTreeNode) {
                    val psiManager = PsiManager.getInstance(project)
                    val virtualFile = it.virtualFile
                    if (virtualFile != null) {
                        val psiFile = psiManager.findFile(virtualFile)
                        if (psiFile is PsiJavaFile) {
                            targetClassesList.add("${psiFile.packageName}.${it.name}")
                        } else {
                            throw IllegalArgumentException("The file is not a Java file: ${it.name}")
                        }
                    } else {
                        throw IllegalArgumentException("Virtual File not found for ${it.name}")
                    }
                }
            }
        }
        return targetClassesList.joinToString(",")
    }

    private fun handleMultipleNodes(navigatableArray: Array<out Navigatable>): String {
        return "TODO"
    }

    private fun runCommand(project: Project, command: String) {
        try {
            val terminalView = TerminalView.getInstance(project)
            val window = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
            val contentManager = window?.contentManager

            val widget = when (val content = contentManager?.findContent("myTabName")) {
                null -> terminalView.createLocalShellWidget(project.basePath, "myTabName")
                else -> TerminalView.getWidgetByContent(content) as ShellTerminalWidget
            }

            widget.executeCommand(command)
        } catch (e: IOException) {
            val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
            console.print("Cannot run command in local terminal. Error:$e", ConsoleViewContentType.NORMAL_OUTPUT)
        }
    }

//    private fun getSourceRoot(module: Module): VirtualFile {
//        val moduleRootManager = ModuleRootManager.getInstance(module)
//        val testSources = moduleRootManager.getSourceRoots(JavaSourceRootType.TEST_SOURCE)
//        val testSourceRoot: VirtualFile =
//            if (testSources.size == 0) {
//                //TODO: create test source root for Gradle
////                Messages.showMessageDialog(
////                    project,
////                    "No test source root found. Do you want to create it at src/test/java?",
////                    "Create Test Source Root",
////                    Messages.getInformationIcon()
////                )
//                // TODO: check if source root path exists
//                createTestSourceRoot(moduleRootManager, "src/test/java")
//            } else {
//                testSources[0]
//            }
//        return testSourceRoot
//    }

    private fun getPackageName(psiClass: PsiClass): String {
        val psiFile = psiClass.containingFile
        if (psiFile is PsiJavaFile) {
            return psiFile.packageName
        }
        throw IllegalArgumentException("The package name class cannot be found")
    }

    private fun checkRequiredData() {
        if (project == null)
            throw IllegalStateException("There is some required data null")
    }

    private fun getModule(psiElement: PsiElement): Module {
        val moduleNullable = ModuleUtil.findModuleForPsiElement(psiElement)
        checkModule(moduleNullable)
        return moduleNullable as Module
    }

    private fun checkModule(module: Module?) {
        if (module == null)
            throw IllegalStateException("Module was not found!")
    }
}