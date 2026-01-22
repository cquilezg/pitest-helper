package com.cquilez.pitesthelper.infrastructure.ui

import com.cquilez.pitesthelper.application.port.out.BuildSystemPort
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.infrastructure.AppMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.ui.EditorTextField
import com.intellij.util.IconUtil
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.ui.InplaceButton
import com.intellij.ui.JBColor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.Action
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

class MutationCoverageDialog(
    val mutationCoverageOptions: MutationCoverageOptions,
    private val buildSystemPort: BuildSystemPort
) : DialogWrapper(true) {
    private val commandEditorTextField = createCommandEditorTextField()

    init {
        title = AppMessagesBundle.message("ui.dialog.mutationCoverage.title")
        init()
        centerDialog()
        pack()
        centerRelativeToParent()
    }

    private fun centerDialog() {
        setSize(
            Toolkit.getDefaultToolkit().screenSize.width / 2,
            size.height
        )
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                browserLink(
                    AppMessagesBundle.message("ui.dialog.mutationCoverage.setupLink"),
                    "https://github.com/cquilezg/pitest-helper?tab=readme-ov-file#set-up-your-project"
                ).align(AlignX.RIGHT)
            }
            if (mutationCoverageOptions.errors.isNotEmpty()) {
                row {
                    cell(JPanel(BorderLayout()).apply {
                        background = JBColor(Color(255, 205, 210), Color(92, 43, 43))
                        val errorsPanel = JPanel().apply {
                            layout = BoxLayout(this, BoxLayout.Y_AXIS)
                            isOpaque = false
                            border = JBUI.Borders.empty(10)
                            add(JBLabel(AppMessagesBundle.message("ui.dialog.mutationCoverage.errors.header")).apply {
                                foreground = JBColor(Color(183, 28, 28), Color(255, 138, 128))
                            })
                            mutationCoverageOptions.errors.forEach { error ->
                                add(JBLabel(AppMessagesBundle.message("ui.dialog.mutationCoverage.errors.item", error)).apply {
                                    foreground = JBColor(Color(183, 28, 28), Color(255, 138, 128))
                                })
                            }
                        }
                        add(errorsPanel, BorderLayout.CENTER)
                    }).align(AlignX.FILL)
                }
            }
            row(AppMessagesBundle.message("ui.dialog.mutationCoverage.targetClasses")) {
                textField()
                    .align(AlignX.FILL)
                    .bindText(mutationCoverageOptions::targetClasses)
                    .applyToComponent {
                        document.addDocumentListener(disposable) {
                            mutationCoverageOptions.targetClasses = normalizeInput(text)
                            updateCommandTextArea()
                        }
                    }
            }
            row(AppMessagesBundle.message("ui.dialog.mutationCoverage.targetTests")) {
                textField()
                    .align(AlignX.FILL)
                    .bindText(mutationCoverageOptions::targetTests)
                    .applyToComponent {
                        document.addDocumentListener(disposable) {
                            mutationCoverageOptions.targetTests = normalizeInput(text)
                            updateCommandTextArea()
                        }
                    }
            }
            row(AppMessagesBundle.message("ui.dialog.mutationCoverage.runCommand")) {
                val scaledIcon = IconUtil.scale(AllIcons.Actions.Copy, null, 1.3f)
                val copyButton = InplaceButton(AppMessagesBundle.message("ui.dialog.mutationCoverage.button.copyToClipboard"), scaledIcon) {
                    val selection = StringSelection(commandEditorTextField.text)
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
                }.apply {
                    val buttonSize = Dimension(32, 32)
                    preferredSize = buttonSize
                    minimumSize = buttonSize
                    val hoverBackground = JBUI.CurrentTheme.ActionButton.hoverBackground()
                    addMouseListener(object : MouseAdapter() {
                        override fun mouseEntered(e: MouseEvent?) {
                            isOpaque = true
                            background = hoverBackground
                        }
                        override fun mouseExited(e: MouseEvent?) {
                            isOpaque = false
                        }
                    })
                }
                cell(JPanel(BorderLayout()).apply {
                    isOpaque = false
                    val buttonPanel = JPanel(BorderLayout()).apply {
                        isOpaque = false
                        border = JBUI.Borders.empty()
                        add(copyButton, BorderLayout.EAST)
                    }
                    add(buttonPanel, BorderLayout.NORTH)
                    val command = buildCommand()
                    commandEditorTextField.text = command
                    // Set accessible name to raw command for UI testing (soft wrap adds visual chars)
                    commandEditorTextField.accessibleContext.accessibleName = command
                    commandEditorTextField.border = JBUI.Borders.customLine(JBColor.border())
                    add(commandEditorTextField, BorderLayout.CENTER)
                }).align(AlignX.FILL + AlignY.FILL)
            }.topGap(TopGap.SMALL).resizableRow()
        }.apply {
            minimumSize = Dimension(600, 200)
        }
    }

    /**
     * Changed OK text button by Run
     */
    override fun createActions(): Array<Action> {
        val actions: Array<Action> = super.createActions()
        for (action in actions) {
            if ("OK" == action.getValue(Action.NAME).toString()) {
                action.putValue(Action.NAME, AppMessagesBundle.message("ui.dialog.mutationCoverage.button.run"))
            }
        }
        return actions
    }

    private fun buildCommand() = buildSystemPort.buildCommand(mutationCoverageOptions)

    private fun updateCommandTextArea() {
        val command = buildCommand()
        commandEditorTextField.text = command
        commandEditorTextField.accessibleContext.accessibleName = command
    }

    private fun createCommandEditorTextField(): EditorTextField {
        val document = EditorFactory.getInstance().createDocument("")
        return object : EditorTextField(document, null, null, true, false) {
            override fun createEditor(): EditorEx {
                return super.createEditor().apply {
                    settings.isUseSoftWraps = true
                    settings.isLineNumbersShown = false
                    settings.isFoldingOutlineShown = false
                    settings.isAdditionalPageAtBottom = false
                    setVerticalScrollbarVisible(true)
                    setHorizontalScrollbarVisible(false)
                }
            }
        }.apply {
            preferredSize = Dimension(600, 100)
        }
    }

    private fun normalizeInput(text: String): String {
        return text.split(Regex("[,\\s]+"))
            .filter { it.isNotEmpty() }
            .joinToString(",")
    }

    private fun javax.swing.text.Document.addDocumentListener(
        parentDisposable: Disposable,
        onChange: () -> Unit
    ) {
        val listener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = onChange()
            override fun removeUpdate(e: DocumentEvent) = onChange()
            override fun changedUpdate(e: DocumentEvent) = onChange()
        }
        addDocumentListener(listener)
        Disposer.register(parentDisposable) { removeDocumentListener(listener) }
    }
}
