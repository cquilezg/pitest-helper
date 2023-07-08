package com.cquilez.pitesthelper.model

import com.intellij.pom.Navigatable

class CodeItem(val name: String, val qualifiedName: String, val codeItemType: CodeItemType, val navigatable: Navigatable)