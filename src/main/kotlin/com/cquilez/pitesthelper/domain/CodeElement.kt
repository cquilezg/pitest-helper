package com.cquilez.pitesthelper.domain

import java.nio.file.Path

abstract class CodeElement(
    open val path: Path,
    open val qualifiedName: String,
    open val sourceFolder: SourceFolder
) {
    open fun toTargetString(): String = qualifiedName

    companion object {
        fun formatList(codeItems: List<CodeElement>): String =
            codeItems.map { it.toTargetString() }.sorted().joinToString(",")

        fun removeNestedElements(codeElements: List<CodeElement>): List<CodeElement> {
            val packages = codeElements.filterIsInstance<CodePackage>()
            val classes = codeElements.filterIsInstance<CodeClass>()

            val optimizedPackages = packages.filter { pkg ->
                packages.none { other -> other != pkg && pkg.qualifiedName.startsWith("${other.qualifiedName}.") }
            }

            val optimizedClasses = classes.filter { cls ->
                optimizedPackages.none { pkg -> cls.qualifiedName.startsWith("${pkg.qualifiedName}.") }
            }

            return optimizedPackages + optimizedClasses
        }
    }
}


