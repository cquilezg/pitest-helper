package com.cquilez.pitesthelper.infrastructure.adapter.psi

import com.cquilez.pitesthelper.infrastructure.extension.PsiPort
import com.cquilez.pitesthelper.domain.CodeClass
import com.cquilez.pitesthelper.domain.CodeElement
import com.cquilez.pitesthelper.domain.CodePackage
import com.cquilez.pitesthelper.domain.SourceFolder
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiJavaFile
import java.nio.file.Paths

open class JavaPsiAdapter : PsiPort {

    override fun getCodeElement(psiElement: PsiFileSystemItem, sourceFolder: SourceFolder): CodeElement? {
        val nodePath = Paths.get(psiElement.virtualFile.path)

        return when (psiElement) {
            is PsiJavaFile -> {
                psiElement.classes.firstOrNull()?.let { psiClass ->
                    psiClass.qualifiedName?.let { qualifiedName ->
                        CodeClass(
                            path = nodePath,
                            sourceFolder = sourceFolder,
                            qualifiedName = qualifiedName,
                            simpleName = psiClass.name ?: ""
                        )
                    }
                }
            }

            is PsiDirectory -> {
                val javaDirectoryService = JavaDirectoryService.getInstance()
                val javaPackage = javaDirectoryService.getPackage(psiElement)
                if (javaPackage != null && javaPackage.name != null && javaPackage.name!!.isNotBlank()) {
                    var qualifiedName = javaPackage.qualifiedName
                    if (qualifiedName == "") {
                        qualifiedName = javaPackage.name!!
                    }
                    CodePackage(
                        nodePath,
                        qualifiedName,
                        sourceFolder
                    )
                } else {
                    null
                }
            }

            else -> null
        }
    }
}

