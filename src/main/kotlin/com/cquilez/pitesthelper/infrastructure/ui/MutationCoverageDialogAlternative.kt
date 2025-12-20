package com.cquilez.pitesthelper.infrastructure.ui

import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.domain.BuildSystem
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.Action
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

class MutationCoverageDialogAlternative(
    private val project: Project,
    val mutationCoverageOptions: MutationCoverageOptions,
    private val buildSystem: BuildSystem
) : DialogWrapper(true) {
    private val commandTextArea = JBTextArea()

    // Modify options state
    private var enableVerboseLogging = false
    private var skipCleanPhase = false
    private var generateHtmlReport = true
    private var mutationEngine = "Gregor"
    private var coverageThreshold = "0%"
    private var timeoutFactor = "1.25"
    private var maxMutationsPerClass = ""

    init {
        title = "Mutation Coverage"
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
                    "How to setup PITest Helper in your project",
                    "https://github.com/cquilezg/pitest-helper?tab=readme-ov-file#set-up-your-project"
                )
                cell()  // Spacer to push Modify options to the right
                link("Modify options") {
                    createModifyOptionsPopup().showUnderneathOf(it.source as Component)
                }.apply {
                    component.icon = AllIcons.General.GearPlain
                }.align(AlignX.RIGHT)
            }
            // Show errors if any nodes were not found
            if (mutationCoverageOptions.errors.isNotEmpty()) {
                group("Errors") {
                    row {
                        cell(JBTextArea().apply {
                            text = mutationCoverageOptions.errors.joinToString("\n")
                            isEditable = false
                            lineWrap = true
                            wrapStyleWord = true
                            background = JBUI.CurrentTheme.Banner.ERROR_BACKGROUND
                            foreground = JBUI.CurrentTheme.Banner.FOREGROUND
                            rows = minOf(mutationCoverageOptions.errors.size, 5)
                        })
                            .align(AlignX.FILL)
                    }
                }.apply {
                    resizableRow()
                }
            }
            row(if (buildSystem == BuildSystem.MAVEN) "Pre goals:" else "Pre tasks:") {
                textField()
                    .align(AlignX.FILL)
                    .bindText(mutationCoverageOptions::preActions)
                    .onChanged {
                        mutationCoverageOptions.preActions = it.text.trim()
                        updateCommandTextArea()
                    }
            }
            row(if (buildSystem == BuildSystem.MAVEN) "Post goals:" else "Post tasks:") {
                textField()
                    .align(AlignX.FILL)
                    .bindText(mutationCoverageOptions::postActions)
                    .onChanged {
                        mutationCoverageOptions.postActions = it.text.trim()
                        updateCommandTextArea()
                    }
            }
            row("Target Classes:") {
                textField()
                    .align(AlignX.FILL)
                    .bindText(mutationCoverageOptions::targetClasses)
                    .onChanged {
                        mutationCoverageOptions.targetClasses = it.text.replace(" ", "")
                        updateCommandTextArea()
                    }
            }
            row("Target Tests:") {
                textField()
                    .align(AlignX.FILL)
                    .bindText(mutationCoverageOptions::targetTests)
                    .onChanged {
                        mutationCoverageOptions.targetTests = it.text.replace(" ", "")
                        updateCommandTextArea()
                    }
            }
            row("Run command (JBTextArea with icon):") {
                cell(JPanel(BorderLayout()).apply {
                    val textArea = JBTextArea(buildCommand(), 5, 30).apply {
                        isEditable = false
                        lineWrap = true
                        wrapStyleWord = true
                        // Colores adaptativos del tema
                        background = UIUtil.getTextFieldBackground()
                        foreground = UIUtil.getLabelForeground()
                    }

                    val scrollPane = JBScrollPane(textArea)

                    val copyButton = JButton(AllIcons.Actions.Copy).apply {
                        toolTipText = "Copy to clipboard"
                        preferredSize = Dimension(30, 30)
                        addActionListener {
                            CopyPasteManager.getInstance().setContents(StringSelection(textArea.text))
                        }
                    }

                    // Botón en esquina superior derecha
                    scrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, copyButton)

                    add(scrollPane, BorderLayout.CENTER)
                })
                    .align(AlignX.FILL + AlignY.FILL)
                    .resizableColumn()
            }.resizableRow()
            row("Run command extendable:") {
                cell(ExtendableTextField().apply {
                    text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."
                    isEditable = false

                    // Añadir botón de copiar
                    val copyExtension = ExtendableTextComponent.Extension.create(
                        AllIcons.Actions.Copy,
                        AllIcons.Actions.Copy,
                        "Copy to clipboard"
                    ) {
                        CopyPasteManager.getInstance().setContents(StringSelection(text))
                    }

                    addExtension(copyExtension)
                })
                    .align(AlignX.FILL)
                    .resizableColumn()
            }
            row("Run command:") {
                val document = EditorFactory.getInstance().createDocument(buildCommand())
                cell(
                    EditorTextField(
                        document,
                        project,
                        FileTypes.PLAIN_TEXT,
                        true,   // isViewer = true (solo lectura)
                        false   // oneLineMode = false (multilínea)
                    ).apply {
                        addSettingsProvider { editor ->
                            editor.settings.isUseSoftWraps = true
                            editor.setVerticalScrollbarVisible(true)
                            editor.contentComponent.border = JBUI.Borders.empty(10)

                            // Aplicar colores del tema actual
                            editor.colorsScheme.apply {
                                setColor(EditorColors.CARET_ROW_COLOR, null)  // Sin highlight de línea
                            }
                            editor.backgroundColor = UIUtil.getTextFieldBackground()
                            editor.contentComponent.foreground = UIUtil.getLabelForeground()
                        }
                    })
                    .align(AlignX.FILL + AlignY.FILL)
                    .resizableColumn()  // Permite expansión horizontal
                    .applyToComponent {
                        // Altura mínima pero expansible
                        minimumSize = Dimension(400, 150)
                    }
            }.resizableRow()
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
                action.putValue(Action.NAME, "Run")
            }
        }
        return actions
    }

    /**
     * Creates a popup with modify options similar to Run Configurations dialog
     */
    private fun createModifyOptionsPopup(): JBPopup {
        val optionsPanel = panel {
            // Checkbox options
            row {
                checkBox("Enable verbose logging")
                    .bindSelected(::enableVerboseLogging)
                    .comment("Show detailed PITest output in console")
            }
            row {
                checkBox("Skip clean phase")
                    .bindSelected(::skipCleanPhase)
                    .comment("Skip clean before running mutations")
            }
            row {
                checkBox("Generate HTML report")
                    .bindSelected(::generateHtmlReport)
                    .comment("Create HTML report after execution")
            }

            separator()

            // ComboBox options
            row("Mutation engine:") {
                comboBox(listOf("Gregor", "Descartes"))
                    .bindItem(::mutationEngine.toNullableProperty())
                    .comment("Select mutation engine to use")
            }
            row("Coverage threshold:") {
                comboBox(listOf("0%", "50%", "75%", "100%"))
                    .bindItem(::coverageThreshold.toNullableProperty())
                    .comment("Minimum coverage required")
            }

            separator()

            // TextField options
            row("Timeout factor:") {
                textField()
                    .bindText(::timeoutFactor)
                    .comment("Multiplier for test timeout (default: 1.25)")
                    .columns(10)
            }
            row("Max mutations per class:") {
                textField()
                    .bindText(::maxMutationsPerClass)
                    .comment("Leave empty for unlimited")
                    .columns(10)
            }
        }.apply {
            border = JBUI.Borders.empty(10)
        }

        return JBPopupFactory.getInstance()
            .createComponentPopupBuilder(optionsPanel, null)
            .setTitle("Modify Options")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setFocusable(true)
            .createPopup()
    }

    private fun buildCommand() = mutationCoverageOptions.buildCommand(buildSystem)

    private fun updateCommandTextArea() {
        commandTextArea.text = buildCommand()
    }
}

