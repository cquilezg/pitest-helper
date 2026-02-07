package com.cquilez.pitesthelper.ui.fixtures

import com.intellij.driver.sdk.ui.QueryBuilder
import com.intellij.driver.sdk.ui.components.ComponentData
import com.intellij.driver.sdk.ui.components.UiComponent
import com.intellij.driver.sdk.ui.components.common.IdeaFrameUI
import com.intellij.driver.sdk.ui.components.elements.JTextFieldUI
import com.intellij.driver.sdk.ui.components.elements.textField
import com.intellij.driver.sdk.ui.xQuery

fun IdeaFrameUI.mutationCoverageDialog(
    locator: QueryBuilder.() -> String = { byTitle("Mutation Coverage") },
    action: MutationCoverageDialogUI.() -> Unit = {},
): MutationCoverageDialogUI = x(MutationCoverageDialogUI::class.java, locator).apply(action)

class MutationCoverageDialogUI(data: ComponentData) : UiComponent(data) {

    val modifyOptions: UiComponent
        get() = x(xQuery { byText("Modify Options") })

    val preGoalsField: JTextFieldUI
        get() = textField(xQuery {
            and(
                byClass("JBTextField"),
                byAccessibleName("Pre Goals:")
            )
        })

    val postGoalsField: JTextFieldUI
        get() = textField(xQuery {
            and(
                byClass("JBTextField"),
                byAccessibleName("Post Goals:")
            )
        })

    val commandTextArea: EditorTextFieldUI
        get() = editorTextField { byClass("EditorTextField") }

    val runButton: UiComponent
        get() = x(xQuery { and(byClass("JButton"), byText("Run")) })

    val cancelButton: UiComponent
        get() = x(xQuery { byText("Cancel") })

    val helpLink: UiComponent
        get() = x(xQuery {
            and(
                byClass("BrowserLink"),
                byText("How to set up PITest Helper in your project")
            )
        })

    val targetClassesLabel: UiComponent
        get() = x(xQuery { and(byClass("JLabel"), byText("Target Classes:")) })

    val targetClassesField: JTextFieldUI
        get() = textField(xQuery { and(byClass("JBTextField"), byAccessibleName("Target Classes:")) })

    val targetTestsLabel: UiComponent
        get() = x(xQuery { and(byClass("JLabel"), byText("Target Tests:")) })

    val targetTestsField: JTextFieldUI
        get() = textField(xQuery { and(byClass("JBTextField"), byAccessibleName("Target Tests:")) })

    val runCommandLabel: UiComponent
        get() = x(xQuery { and(byClass("JLabel"), byText("Run Command:")) })

    /**
     * Returns the errors section panel (JPanel that contains the given header text).
     * @param headerText default "Please, check the following errors:"
     */
    fun errorsSection(headerText: String = "Please, check the following errors:"): UiComponent =
        xx(xQuery { byClass("JPanel") }).list().find { panel ->
            panel.getAllTexts().any { t -> t.toString().contains(headerText) }
        } ?: error("Errors section (panel containing \"$headerText\") not found.")
}