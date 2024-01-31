package com.cquilez.pitesthelper.actions

import com.cquilez.pitesthelper.exception.PitestHelperException
import com.cquilez.pitesthelper.model.CodeItem
import com.cquilez.pitesthelper.model.CodeItemType
import com.cquilez.pitesthelper.model.MutationCoverage
import com.cquilez.pitesthelper.model.MutationCoverageData
import com.cquilez.pitesthelper.services.*
import com.cquilez.pitesthelper.ui.MutationCoverageDialog
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.TestOnly
import org.jetbrains.jps.model.java.JavaSourceRootType

class RunMutationCoverageAction : DumbAwareAction() {

    private val stringBuilder = StringBuilder()
    private var project: Project? = null
    private val helpMessage = "Please select only Java classes, packages or a module folder containing Java source code."

    /**
     * Object only needed for unit tests
     */
    @TestOnly
    companion object {
        var mutationCoverageData: MutationCoverageData? = null
    }

    /**
     * Update action in menu.
     *
     * @param event Action event.
     */
    override fun update(@NotNull event: AnActionEvent) {
        var visible = false
        project = event.project
        if (project != null) {
            val psiFile = event.getData(CommonDataKeys.PSI_FILE)
            // TODO: Check if the directory is inside of a module and in a source root
            if (psiFile != null && ClassService.isCodeFile(psiFile)) {
                visible = true
            } else {
                val navigatableArray = event.getData(CommonDataKeys.NAVIGATABLE_ARRAY)
                if (!navigatableArray.isNullOrEmpty()) {
                    visible = true
                }
            }
        }

        event.presentation.isEnabledAndVisible = visible
    }

    /**
     * Read nodes, build mutation coverage command and show dialog
     */
    override fun actionPerformed(@NotNull event: AnActionEvent) {
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)

        val project = project as Project
        val projectService = project.service<MyProjectService>()

        stringBuilder.clear()
        val navigatableArray = event.getData(CommonDataKeys.NAVIGATABLE_ARRAY)

        try {
            val mutationCoverageData: MutationCoverageData =
                if (navigatableArray != null) {
                    processMultipleNodes(project, navigatableArray, projectService)
                } else if (psiFile != null && ClassService.isCodeFile(psiFile)) {
                    processSingleNode(psiFile)
                } else {
                    throw PitestHelperException("No elements found")
                }
            showMutationCoverageDialog(project, mutationCoverageData)
        } catch (e: PitestHelperException) {
            Messages.showErrorDialog(project, e.message, "Unable To Run Mutation Coverage")
        }
    }

    private fun processMultipleNodes(
        project: Project,
        navigatableArray: Array<Navigatable>,
        projectService: MyProjectService
    ): MutationCoverageData {
        if (navigatableArray.isNotEmpty()) {
            if (navigatableArray.size > 1) {
                checkAllElementsAreInSameModule(project, navigatableArray)
            }
            val module = getModuleForNavigatable(project, navigatableArray[0])
            val mutationCoverage = processNavigatables(projectService, project, module, navigatableArray)
            syncClassesAndPackages(project, module, projectService, mutationCoverage)
            return PITestService.buildMutationCoverageCommand(module, mutationCoverage)
        }
        throw PitestHelperException("There are no elements to process")
    }

    private fun syncClassesAndPackages(
        project: Project,
        module: Module,
        projectService: MyProjectService,
        mutationCoverage: MutationCoverage
    ) {
        val dumb = DumbService.getInstance(project).isDumb
        mutationCoverage.testSource.forEach {
            if (it.codeItemType == CodeItemType.CLASS) {
                processClassCodeItem(
                    it,
                    dumb,
                    projectService,
                    project,
                    module,
                    mutationCoverage.normalSource,
                    TestService.getClassUnderTestName(it.name)
                )
            } else if (it.codeItemType == CodeItemType.PACKAGE) {
                processPackageCodeItem(
                    project,
                    it,
                    mutationCoverage.normalSource,
                    projectService.getMainSourceFolders(module)
                )
            }
        }
        mutationCoverage.normalSource.forEach {
            if (it.codeItemType == CodeItemType.CLASS) {
                processClassCodeItem(
                    it,
                    dumb,
                    projectService,
                    project,
                    module,
                    mutationCoverage.testSource,
                    it.name + "Test"
                )
            } else if (it.codeItemType == CodeItemType.PACKAGE) {
                processPackageCodeItem(
                    project,
                    it,
                    mutationCoverage.testSource,
                    projectService.getTestSourceFolders(module)
                )
            }
        }
    }

    private fun processPackageCodeItem(
        project: Project,
        it: CodeItem,
        sourceList: MutableList<CodeItem>,
        sourceFolderList: List<SourceFolder>
    ) {
        if (isPackageInSourceFolderList(
                project,
                it.qualifiedName,
                sourceFolderList
            )
        ) {
            if (!isInPackage(sourceList, it.qualifiedName)) {
                sourceList.add(it)
            }
        } else {
            throw PitestHelperException("Package not found!: ${it.qualifiedName}")
        }
    }

    private fun processClassCodeItem(
        it: CodeItem,
        dumb: Boolean,
        projectService: MyProjectService,
        project: Project,
        module: Module,
        sourceList: MutableList<CodeItem>,
        targetClassName: String
    ) {
        val targetClassQualifiedName: String = if (!dumb) {
            val psiClasses = projectService.findClassesInModule(targetClassName, project, module)
            // TODO: validate classes are from test source root
            checkExistingClass(psiClasses, it.qualifiedName, targetClassName)
            val psiClass = if (psiClasses.size == 1) {
                psiClasses[0]
            } else {
                var psiClass =
                    findClassInSamePackage(psiClasses, getPackageNameFromQualifiedName(it.qualifiedName))
                if (psiClass == null) {
                    psiClass =
                        findBestCandidateClass(psiClasses, getPackageNameFromQualifiedName(it.qualifiedName))
                }
                if (psiClass == null) {
                    throw PitestHelperException(
                        "Class under test is not in the same package. Unable to find valid class. " +
                                "Candidates are: ${
                                    psiClasses.joinToString(", ",
                                        transform = {
                                            PITestService.buildFullClassName(
                                                ClassService.getPackageName(it),
                                                it.name!!
                                            )
                                        })
                                }"
                    )
                }
                psiClass
            }
            psiClass.qualifiedName!!
        } else {
            PITestService.buildFullClassName(getPackageNameFromQualifiedName(it.qualifiedName), targetClassName)
        }
        if (!isInPackage(sourceList, targetClassQualifiedName)) {
            sourceList.add(
                CodeItem(
                    targetClassName,
                    targetClassQualifiedName,
                    CodeItemType.CLASS,
                    it.navigatable
                )
            )
        }
    }

    private fun findBestCandidateClass(psiClasses: Array<PsiClass>, classQualifiedName: String): PsiClass? {
        var psiClass: PsiClass? = null
        psiClasses.forEach {
            val candidateClassPackageName = ClassService.getPackageName(it)
            if (PsiNameHelper.isSubpackageOf(classQualifiedName, candidateClassPackageName)
                || PsiNameHelper.isSubpackageOf(candidateClassPackageName, classQualifiedName)
            ) {
                if (psiClass == null) {
                    psiClass = it
                } else {
                    return null
                }
            }
        }
        return psiClass
    }

    private fun isPackageInSourceFolderList(
        project: Project,
        qualifiedName: String,
        sourceFolderList: List<SourceFolder>
    ): Boolean {
        val psiManager = PsiManager.getInstance(project)
        for (sourceRoot in sourceFolderList) {
            val packageDirectory = sourceRoot.file!!.findFileByRelativePath(qualifiedName.replace('.', '/'))
            if (packageDirectory != null && packageDirectory.isDirectory) {
                val psiDirectory = psiManager.findDirectory(packageDirectory)
                if (psiDirectory != null) {
                    val psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory)
                    if (psiPackage != null && psiPackage.qualifiedName == qualifiedName) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun getPackageNameFromQualifiedName(qualifiedName: String): String {
        val lastDotIndex = qualifiedName.lastIndexOf('.')
        return if (lastDotIndex != -1) {
            qualifiedName.substring(0, lastDotIndex)
        } else ""
    }

    private fun processSingleNode(psiFile: PsiFile): MutationCoverageData {
        val psiClass = ClassService.getPublicClass(psiFile)
        val targetClasses = PITestService.getTestClassQualifiedName(psiClass)
        val targetTests = PITestService.extractTargetTestsByPsiClass(psiClass)
        val module = getModuleFromElement(psiFile)
        return MutationCoverageData(module, listOf(targetClasses), listOf(targetTests))
    }

    /**
     * Shows Mutation Coverage dialog and runs Maven command when OK button is pressed.
     * Does not show the dialog if you are running plugin tests.
     */
    private fun showMutationCoverageDialog(project: Project, mutationCoverageData: MutationCoverageData) {
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            val dialog = MutationCoverageDialog(mutationCoverageData)
            dialog.show()
            if (dialog.isOK) {
                MavenService.runMavenCommand(
                    project,
                    mutationCoverageData.module,
                    listOf("test-compile", "pitest:mutationCoverage"),
                    MavenService.buildPitestArgs(dialog.data.targetClasses, dialog.data.targetTests)
                )
            }
        } else {
            RunMutationCoverageAction.mutationCoverageData = mutationCoverageData
        }
    }

    private fun getModuleForNavigatable(project: Project, navigatable: Navigatable): Module {
        val module = when (navigatable) {
            is ClassTreeNode -> {
                ModuleUtilCore.findModuleForFile((navigatable).virtualFile!!, project)
            }

            is PsiDirectoryNode -> {
                ModuleUtilCore.findModuleForFile((navigatable.value).virtualFile, project)
            }

            else -> {
                null
            }
        }
            ?: throw PitestHelperException("There is/are elements not supported. $helpMessage")
        return module
    }

    private fun checkAllElementsAreInSameModule(project: Project, navigatableArray: Array<Navigatable>) {
        if (navigatableArray.isEmpty()) {
            throw PitestHelperException("There are no elements to process")
        }
        var module: Module? = null
        navigatableArray.forEach {
            val newModule: Module = getModuleForNavigatable(project, it)
            if (module == null) {
                module = newModule
            } else if (module != newModule) {
                throw PitestHelperException("You cannot choose elements from different modules")
            }
        }
    }

    private fun processNavigatables(
        projectService: MyProjectService,
        project: Project,
        module: Module,
        navigatableArray: Array<out Navigatable>
    ): MutationCoverage {
        val javaDirectoryService = JavaDirectoryService.getInstance()
        val mutationCoverage = MutationCoverage(mutableListOf(), mutableListOf())
        if (navigatableArray.isNotEmpty()) {
            navigatableArray.forEach { navigatable ->
                processNavigatable(navigatable, javaDirectoryService, module, projectService, project, mutationCoverage)
            }
        }
        return mutationCoverage
    }

    private fun processNavigatable(
        navigatable: Navigatable,
        javaDirectoryService: JavaDirectoryService,
        module: Module,
        projectService: MyProjectService,
        project: Project,
        mutationCoverage: MutationCoverage
    ) {
        if (navigatable is PsiDirectoryNode) {
            processDirectory(javaDirectoryService, navigatable, module, projectService, project, mutationCoverage)
        } else if (navigatable is ClassTreeNode) {
            processClass(projectService, project, navigatable, module, mutationCoverage)
        }
    }

    private fun processClass(
        projectService: MyProjectService,
        project: Project,
        navigatable: ClassTreeNode,
        module: Module,
        mutationCoverage: MutationCoverage
    ) {
        val sourceRoot: VirtualFile? = projectService.getSourceRoot(project, navigatable.virtualFile!!)
        val psiFile = getPsiFile(project, navigatable)
        if (psiFile is PsiJavaFile) {
            val psiClass = ClassService.getPublicClass(psiFile)
            addIfNotPresent(
                projectService, module, sourceRoot!!, mutationCoverage,
                CodeItem(psiClass.name!!, psiClass.qualifiedName!!, CodeItemType.CLASS, navigatable)
            )
        }
    }

    private fun processDirectory(
        javaDirectoryService: JavaDirectoryService,
        navigatable: PsiDirectoryNode,
        module: Module,
        projectService: MyProjectService,
        project: Project,
        mutationCoverage: MutationCoverage
    ) {
        val sourceRoot: VirtualFile?
        val packageName: String
        var qualifiedName: String
        val javaPackage = javaDirectoryService.getPackage(navigatable.value)
        if (javaPackage != null && javaPackage.name != null && javaPackage.name!!.isNotBlank()) {
            val srcMainJavaFolder = MavenService.locateSrcMainJava(module) ?: throw PitestHelperException("The folder src/main/java was not found. There is no source folder to find mutations.")
            packageName = javaPackage.name ?: getBasePackage(srcMainJavaFolder)
            qualifiedName = javaPackage.qualifiedName
            if (qualifiedName == "") {
                qualifiedName = packageName
            }
            sourceRoot = projectService.getSourceRoot(project, navigatable.virtualFile!!)
            addIfNotPresent(
                projectService, module, sourceRoot!!, mutationCoverage,
                CodeItem(packageName, qualifiedName, CodeItemType.PACKAGE, navigatable)
            )
        } else {
            val sourceFolders = projectService.getFilteredSourceFolders(module) {
                it.rootType is JavaSourceRootType && !projectService.isAutogeneratedSourceFolder(it)
            }
            val directoryPath = navigatable.virtualFile?.toNioPath()
            if (directoryPath != null) {
                val sourceFolderFound = sourceFolders.stream().filter {
                    it.file?.toNioPath()?.startsWith(directoryPath) ?: false
                }.findFirst()
                if (sourceFolderFound.isPresent && sourceFolderFound.get().file != null) {
                    packageName = getBasePackage(sourceFolderFound.get().file!!)
                    diffElements(mutationCoverage.normalSource, CodeItem(packageName, packageName, CodeItemType.PACKAGE, navigatable))
                } else {
                    throw PitestHelperException("The directory appears to contain no code. $helpMessage")
                }
            } else {
                throw PitestHelperException("The directory appears to contain no code. $helpMessage")
            }
        }
    }

    fun getBasePackage(rootFile: VirtualFile): String {
        var lastNode = rootFile
        while (lastNode.children.size == 1) {
            lastNode = lastNode.children[0]
        }
        val relativePath = VfsUtil.getRelativePath(lastNode, rootFile)
        return relativePath?.replace("/", ".") ?: ""
    }

    private fun addIfNotPresent(
        projectService: MyProjectService,
        module: Module,
        sourceRoot: VirtualFile,
        mutationCoverage: MutationCoverage,
        codeItem: CodeItem
    ) {
        val codeItemList =
            if (projectService.isTestSourceRoot(module, sourceRoot.canonicalFile!!)) {
                mutationCoverage.testSource
            } else {
                mutationCoverage.normalSource
            }
        diffElements(codeItemList, codeItem)
    }

    private fun diffElements(
        codeItemList: MutableList<CodeItem>,
        codeItem: CodeItem
    ) {
        if (!isInPackage(codeItemList, codeItem.qualifiedName)) {
            val descendantCodeList = getChildCode(codeItemList, codeItem.qualifiedName)
            codeItemList.removeAll(descendantCodeList)
            codeItemList.add(codeItem)
        }
    }

    private fun getChildCode(codeItemList: MutableList<CodeItem>, newItemQualifiedName: String): List<CodeItem> {
        return codeItemList.filter {
            PsiNameHelper.isSubpackageOf(it.qualifiedName, newItemQualifiedName)
        }
    }

    private fun isInPackage(
        codeItemList: List<CodeItem>,
        newItemPackage: String
    ): Boolean {
        codeItemList.forEach {
            val existingItemPackage = it.qualifiedName
            if (PsiNameHelper.isSubpackageOf(newItemPackage, existingItemPackage)) {
                return true
            }
        }
        return false
    }

    private fun getPsiFile(project: Project, classTreeNode: ClassTreeNode): PsiFile {
        val psiManager = PsiManager.getInstance(project)
        val virtualFile = classTreeNode.virtualFile
        if (virtualFile != null) {
            val psiFile = psiManager.findFile(virtualFile)
            if (psiFile != null) {
                return psiFile
            }
        }
        throw PitestHelperException("There are selected elements not supported: ${classTreeNode.name}. $helpMessage")
    }

    private fun findClassInSamePackage(psiClasses: Array<PsiClass>, packageName: String): PsiClass? {
        return psiClasses.firstOrNull {
            val psiFile = it.containingFile
            if (psiFile is PsiJavaFile) {
                psiFile.packageName == packageName
            } else {
                false
            }
        }
    }

    private fun getModuleFromElement(psiElement: PsiElement): Module {
        val moduleNullable = ModuleUtil.findModuleForPsiElement(psiElement)
        checkModule(moduleNullable)
        return moduleNullable as Module
    }

    private fun checkModule(module: Module?) {
        if (module == null)
            throw PitestHelperException("Module was not found!")
    }

    private fun checkExistingClass(psiClasses: Array<PsiClass>, className: String, testClassName: String) {
        if (psiClasses.isEmpty())
            throw PitestHelperException("There is no test class found for: ${className}. Test not found: ${testClassName}. A test class need to have the suffix Test.")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}