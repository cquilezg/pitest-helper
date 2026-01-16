package com.cquilez.pitesthelper.infrastructure.adapter.psi

import com.cquilez.pitesthelper.domain.CodeClass
import com.cquilez.pitesthelper.domain.CodeElement
import com.cquilez.pitesthelper.domain.SourceFolder
import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Paths

class KotlinPsiAdapter : JavaPsiAdapter() {

    override fun getCodeElement(psiElement: PsiFileSystemItem, sourceFolder: SourceFolder): CodeElement? {
        val javaResult = super.getCodeElement(psiElement, sourceFolder)
        if (javaResult != null) {
            return javaResult
        }

        return when (psiElement) {
            is KtFile -> {
                val nodePath = Paths.get(psiElement.virtualFile.path)
                psiElement.classes.firstOrNull()?.let { ktClass ->
                    ktClass.qualifiedName?.let { qualifiedName ->
                        CodeClass(
                            path = nodePath,
                            sourceFolder = sourceFolder,
                            qualifiedName = qualifiedName,
                            simpleName = ktClass.name ?: ""
                        )
                    }
                }
            }

            else -> null
        }
    }
}

