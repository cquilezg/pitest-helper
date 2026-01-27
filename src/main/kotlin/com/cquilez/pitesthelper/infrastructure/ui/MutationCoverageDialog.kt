package com.cquilez.pitesthelper.infrastructure.ui

import com.cquilez.pitesthelper.application.port.out.BuildSystemPort
import com.cquilez.pitesthelper.domain.BuildSystem
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.infrastructure.AppMessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.Disposer
import com.intellij.ui.EditorTextField
import com.intellij.ui.InplaceButton
import com.intellij.ui.JBColor
import com.intellij.ui.components.DropDownLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.*
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.Action
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.border.AbstractBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

private class EmptyIcon(private val width: Int, private val height: Int) : Icon {
    override fun paintIcon(c: java.awt.Component?, g: Graphics?, x: Int, y: Int) {
        // Empty - draws nothing
    }

    override fun getIconWidth(): Int = width
    override fun getIconHeight(): Int = height
}

private class RoundedBorder(
    private val radius: Int = JBUI.scale(5),
    private val color: Color = UIManager.getColor("TextField.borderColor")
        ?: (if (UIUtil.isUnderDarcula()) Color(100, 100, 100) else Color(150, 150, 150)),
    private val thickness: Int = 1
) : AbstractBorder() {
    override fun paintBorder(c: java.awt.Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2.color = color
        g2.stroke = java.awt.BasicStroke(thickness.toFloat())
        val shape = RoundRectangle2D.Float(
            x.toFloat() + thickness / 2f,
            y.toFloat() + thickness / 2f,
            (width - thickness).toFloat(),
            (height - thickness).toFloat(),
            radius.toFloat(),
            radius.toFloat()
        )
        g2.draw(shape)
    }

    override fun getBorderInsets(c: java.awt.Component): java.awt.Insets {
        val padding = JBUI.scale(6)
        return JBUI.insets(padding, padding, padding, padding)
    }
}

private data class ModifyOption(
    val text: String,
    val isSelected: () -> Boolean,
    val onToggle: (Boolean) -> Unit
)

class MutationCoverageDialog(
    val mutationCoverageOptions: MutationCoverageOptions,
    private val buildSystemPort: BuildSystemPort
) : DialogWrapper(true) {
    private val commandEditorTextField = createCommandEditorTextField()

    private var showPreGoals = mutationCoverageOptions.preActions.isNotBlank()
    private var showPostGoals = mutationCoverageOptions.postActions.isNotBlank()

    private var mainPanel: DialogPanel? = null
    private var scrollPane: JBScrollPane? = null

    init {
        title = AppMessagesBundle.message("ui.dialog.mutationCoverage.title")
        init()
        centerDialog()
        pack()
        centerRelativeToParent()
        applyMinimumSize()
    }

    private fun applyMinimumSize() {
        val minWidth = JBUI.scale(500)
        val minHeight = JBUI.scale(400)
        val minDimension = Dimension(minWidth, minHeight)
        window?.minimumSize = minDimension
        rootPane?.minimumSize = minDimension
    }

    private fun centerDialog() {
        setSize(
            Toolkit.getDefaultToolkit().screenSize.width / 2,
            size.height
        )
    }

    override fun createCenterPanel(): JComponent {
        mainPanel = createMainPanel()
        scrollPane = JBScrollPane(mainPanel).apply {
            border = JBUI.Borders.empty()
            viewportBorder = JBUI.Borders.empty()
        }
        return scrollPane!!
    }

    private fun createMainPanel(): DialogPanel {
        return panel {
            row {
                browserLink(
                    AppMessagesBundle.message("ui.dialog.mutationCoverage.setupLink"),
                    "https://github.com/cquilezg/pitest-helper?tab=readme-ov-file#set-up-your-project"
                ).resizableColumn()
                    .align(AlignX.LEFT)
                cell(DropDownLink<String>(AppMessagesBundle.message("ui.dialog.mutationCoverage.modifyOptions")) { createModifyOptionsPopup() })
                    .align(AlignX.RIGHT)
            }

            row {
                cell(JSeparator(SwingConstants.HORIZONTAL))
                    .align(AlignX.FILL)
            }

            if (mutationCoverageOptions.errors.isNotEmpty()) {
                row {
                    val errorBackground = JBColor(Color(255, 205, 210), Color(92, 43, 43))
                    cell(object : JPanel(BorderLayout()) {
                        override fun paintComponent(g: Graphics) {
                            val g2 = g as Graphics2D
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                            g2.color = errorBackground
                            val radius = JBUI.scale(5)
                            g2.fillRoundRect(0, 0, width, height, radius, radius)
                            super.paintComponent(g)
                        }
                    }.apply {
                        isOpaque = false
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

            if (showPreGoals) {
                val preLabel = if (buildSystemPort.getBuildSystem() == BuildSystem.MAVEN)
                    AppMessagesBundle.message("ui.dialog.mutationCoverage.preGoals")
                else
                    AppMessagesBundle.message("ui.dialog.mutationCoverage.preTasks")
                row(preLabel) {
                    textField()
                        .align(AlignX.FILL)
                        .bindText(mutationCoverageOptions::preActions)
                        .applyToComponent {
                            document.addDocumentListener(disposable) {
                                mutationCoverageOptions.preActions = text.trim()
                                updateCommandTextArea()
                            }
                        }
                }
            }

            if (showPostGoals) {
                val postLabel = if (buildSystemPort.getBuildSystem() == BuildSystem.MAVEN)
                    AppMessagesBundle.message("ui.dialog.mutationCoverage.postGoals")
                else
                    AppMessagesBundle.message("ui.dialog.mutationCoverage.postTasks")
                row(postLabel) {
                    textField()
                        .align(AlignX.FILL)
                        .bindText(mutationCoverageOptions::postActions)
                        .applyToComponent {
                            document.addDocumentListener(disposable) {
                                mutationCoverageOptions.postActions = text.trim()
                                updateCommandTextArea()
                            }
                        }
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
                    commandEditorTextField.border = JBUI.Borders.empty()

                    // Wrapper panel with rounded border and padding
                    val textAreaWrapper = object : JPanel(BorderLayout()) {
                        override fun paintComponent(g: Graphics) {
                            val g2 = g as Graphics2D
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                            g2.color = UIUtil.getTextFieldBackground()
                            val radius = JBUI.scale(5)
                            g2.fillRoundRect(0, 0, width, height, radius, radius)
                            super.paintComponent(g)
                        }
                    }.apply {
                        isOpaque = false
                        border = RoundedBorder()
                        add(commandEditorTextField, BorderLayout.CENTER)
                    }

                    add(textAreaWrapper, BorderLayout.CENTER)
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
                    setBorder(JBUI.Borders.empty())
                    contentComponent.border = JBUI.Borders.empty()
                }
            }
        }.apply {
            preferredSize = Dimension(600, 100)
            border = JBUI.Borders.empty()
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

    private fun refreshDialog() {
        mainPanel = createMainPanel()
        scrollPane?.setViewportView(mainPanel)
        scrollPane?.revalidate()
        scrollPane?.repaint()
        applyMinimumSize()
    }

    private fun createModifyOptionsPopup(): JBPopup {
        val options = createModifyOptionsList()
        val step = object : BaseListPopupStep<ModifyOption>(AppMessagesBundle.message("ui.dialog.mutationCoverage.modifyOptions"), options) {
            override fun isSelectable(value: ModifyOption?): Boolean = true

            override fun onChosen(selectedValue: ModifyOption?, finalChoice: Boolean): PopupStep<*>? {
                if (selectedValue != null) {
                    val newState = !selectedValue.isSelected()
                    selectedValue.onToggle(newState)
                    SwingUtilities.invokeLater { refreshDialog() }
                }
                return null
            }

            override fun hasSubstep(selectedValue: ModifyOption?): Boolean = false

            override fun getTextFor(value: ModifyOption?): String = value?.text ?: ""

            override fun getIconFor(value: ModifyOption?): Icon {
                val checkedIcon = AllIcons.Actions.Checked
                return if (value?.isSelected() == true) {
                    checkedIcon
                } else {
                    EmptyIcon(checkedIcon.iconWidth, checkedIcon.iconHeight)
                }
            }
        }

        return JBPopupFactory.getInstance()
            .createListPopup(step)
    }

    private fun createModifyOptionsList(): List<ModifyOption> {
        val buildSystem = buildSystemPort.getBuildSystem()
        return listOf(
            ModifyOption(
                if (buildSystem == BuildSystem.MAVEN)
                    AppMessagesBundle.message("ui.dialog.mutationCoverage.preGoals").removeSuffix(":")
                else
                    AppMessagesBundle.message("ui.dialog.mutationCoverage.preTasks").removeSuffix(":"),
                { showPreGoals },
                { newState ->
                    showPreGoals = newState
                    if (!newState) {
                        mutationCoverageOptions.preActions = ""
                        updateCommandTextArea()
                    }
                }
            ),
            ModifyOption(
                if (buildSystem == BuildSystem.MAVEN)
                    AppMessagesBundle.message("ui.dialog.mutationCoverage.postGoals").removeSuffix(":")
                else
                    AppMessagesBundle.message("ui.dialog.mutationCoverage.postTasks").removeSuffix(":"),
                { showPostGoals },
                { newState ->
                    showPostGoals = newState
                    if (!newState) {
                        mutationCoverageOptions.postActions = ""
                        updateCommandTextArea()
                    }
                }
            )
        )
    }
}
