package com.cquilez.pitesthelper.domain.model

data class CodeItem(
    val name: String,
    val qualifiedName: String,
    val codeItemType: CodeItemType,
    val sourceFolder: SourceFolder? = null
)