package com.cquilez.pitesthelper.ui.fixtures

import com.intellij.driver.model.OnDispatcher
import com.intellij.driver.sdk.ui.Finder
import com.intellij.driver.sdk.ui.QueryBuilder
import com.intellij.driver.sdk.ui.components.ComponentData
import com.intellij.driver.sdk.ui.components.UiComponent
import com.intellij.driver.sdk.ui.components.elements.JTextComponent

fun Finder.editorTextField(init: QueryBuilder.() -> String) = x(EditorTextFieldUI::class.java, init)

class EditorTextFieldUI(data: ComponentData) : UiComponent(data) {

    private val SOFT_WRAP_CHARS = Regex("[⤦⤥]")

    private val textComponent by lazy { driver.cast(component, JTextComponent::class) }

    var text: String
        get() {
            val texts = this.getAllTexts()
            val combinedText = texts.joinToString("")
            return stripSoftWrapChars(combinedText)
        }
        set(value) = driver.withContext(OnDispatcher.EDT) { textComponent.setText(value) }

    private fun stripSoftWrapChars(text: String): String = text.replace(SOFT_WRAP_CHARS, "")
}
