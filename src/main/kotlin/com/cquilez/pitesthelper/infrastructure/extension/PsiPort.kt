package com.cquilez.pitesthelper.infrastructure.extension

import com.cquilez.pitesthelper.domain.CodeElement
import com.cquilez.pitesthelper.domain.SourceFolder
import com.intellij.psi.PsiFileSystemItem

/**
 * Port for extracting code elements from PSI elements.
 * Different implementations can handle different languages (Java, Kotlin, etc.)
 */
fun interface PsiPort {
    /**
     * Extracts a code element from a PSI element.
     *
     * @param psiElement The PSI element to process
     * @param sourceFolder The source folder that contains this element
     * @return A CodeElement (CodeClass or CodePackage) if extraction is successful, null otherwise
     */
    fun getCodeElement(psiElement: PsiFileSystemItem, sourceFolder: SourceFolder): CodeElement?
}