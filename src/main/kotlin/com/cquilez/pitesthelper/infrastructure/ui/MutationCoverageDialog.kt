package com.cquilez.pitesthelper.infrastructure.ui

import com.cquilez.pitesthelper.domain.BuildUnit
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.cquilez.pitesthelper.domain.BuildSystem
import com.intellij.icons.AllIcons
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.ui.components.DropDownLink
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.*
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
import java.awt.geom.RoundRectangle2D
import java.nio.file.Path
import javax.swing.*
import javax.swing.border.AbstractBorder

private data class ModifyOption(
    val text: String,
    val isSelected: () -> Boolean,
    val onToggle: (Boolean) -> Unit
)

private class RoundedBorder(
    private val radius: Int = JBUI.scale(4),
    private val color: Color = UIUtil.getLabelForeground().darker(),
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
        val padding = JBUI.scale(8)
        return JBUI.insets(padding, padding, padding, padding)
    }
}

class MutationCoverageDialog(
    private val project: Project,
    val mutationCoverageOptions: MutationCoverageOptions,
    private val buildSystem: BuildSystem,
    private val buildUnits: List<BuildUnit> = emptyList()
) : DialogWrapper(true) {
    private val commandTextArea = JBTextArea()

    private var selectedBuildUnit: BuildUnit? = mutationCoverageOptions.workingUnit
        ?: if (buildUnits.isNotEmpty()) buildUnits.first() else null

    private var showPreGoals = mutationCoverageOptions.preActions.isNotBlank()
    private var showPostGoals = mutationCoverageOptions.postActions.isNotBlank()

    private var enableVerboseLogging = false

    private var mainPanel: DialogPanel? = null
    private var runAction: Action? = null
    private var commandTextAreaComponent: JBTextArea? = null

    init {
        title = "Mutation Coverage"
        init()
        centerDialog()
        pack()
        centerRelativeToParent()
        setMinimumSize()
    }

    private fun centerDialog() {
        setSize(
            Toolkit.getDefaultToolkit().screenSize.width / 2,
            size.height
        )
    }

    private fun setMinimumSize() {
        val textArea = commandTextAreaComponent
        if (textArea != null) {
            textArea.doLayout()

            val textAreaPreferredHeight = textArea.preferredSize.height
            val buttonHeight = JBUI.scale(30) + JBUI.scale(5) // Button + margin
            val borderPadding = JBUI.scale(16) // Border padding (8 top + 8 bottom)
            val scrollPaneInsets = JBUI.scale(2) // Scroll pane border
            val minTextAreaHeight = textAreaPreferredHeight + buttonHeight + borderPadding + scrollPaneInsets

            val currentSize = size
            val mainPanelSize = mainPanel?.preferredSize
            val otherElementsHeight = (mainPanelSize?.height ?: 0) - minTextAreaHeight

            val minWidth = maxOf(currentSize.width, JBUI.scale(600))
            val minHeight = minTextAreaHeight + maxOf(otherElementsHeight, JBUI.scale(150)) // Ensure space for other UI elements

            window?.setMinimumSize(Dimension(minWidth, minHeight))
        } else {
            window?.setMinimumSize(Dimension(JBUI.scale(600), JBUI.scale(400)))
        }
    }

    override fun createCenterPanel(): JComponent {
        mainPanel = createMainPanel()
        return mainPanel!!
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return runAction?.let { getButton(it) }
    }

    private fun createMainPanel(): DialogPanel {
        return panel {
            row {
                browserLink(
                    "How to setup PITest Helper in your project",
                    "https://github.com/cquilezg/pitest-helper?tab=readme-ov-file#set-up-your-project"
                ).align(AlignX.LEFT)
                cell()  // Spacer
                cell(DropDownLink<String>("Modify options") { createModifyOptionsPopup() })
                    .align(AlignX.RIGHT)
            }

            row {
                cell(JSeparator(SwingConstants.HORIZONTAL))
                    .align(AlignX.FILL)
            }

            if (buildUnits.isNotEmpty()) {
                row {
                    cell()
                    cell(JPanel(BorderLayout(5, 0)).apply {
                        add(JLabel(AllIcons.Nodes.Module), BorderLayout.WEST)
                        add(DropDownLink<String>(getBuildUnitDisplayName()) { createBuildUnitPopup() }, BorderLayout.CENTER)
                    })
                        .align(AlignX.RIGHT)
                }
            }

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

            if (showPreGoals) {
                row(if (buildSystem == BuildSystem.MAVEN) "Pre goals:" else "Pre tasks:") {
                    textField()
                        .align(AlignX.FILL)
                        .bindText(mutationCoverageOptions::preActions)
                        .onChanged {
                            mutationCoverageOptions.preActions = it.text.trim()
                            updateCommandTextArea()
                        }
                }
            }

            if (showPostGoals) {
                row(if (buildSystem == BuildSystem.MAVEN) "Post goals:" else "Post tasks:") {
                    textField()
                        .align(AlignX.FILL)
                        .bindText(mutationCoverageOptions::postActions)
                        .onChanged {
                            mutationCoverageOptions.postActions = it.text.trim()
                            updateCommandTextArea()
                        }
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
            row("Run command:") {
                cell(JPanel(BorderLayout()).apply {
                    val textFieldBorderColor = UIManager.getColor("TextField.borderColor")
                        ?: (if (UIUtil.isUnderDarcula()) java.awt.Color(100, 100, 100) else java.awt.Color(150, 150, 150))

                    val roundedBorder = RoundedBorder(
                        radius = JBUI.scale(4),
                        color = textFieldBorderColor,
                        thickness = 1
                    )

                    val textArea = JBTextArea(buildCommand(), 5, 30).apply {
                        isEditable = false
                        lineWrap = true
                        wrapStyleWord = true
                        background = UIUtil.getTextFieldBackground()
                        setBorder(roundedBorder)
                        isOpaque = true
                    }

                    commandTextAreaComponent = textArea

                    val textAreaPreferredSize = textArea.preferredSize
                    textArea.minimumSize = Dimension(JBUI.scale(200), textAreaPreferredSize.height)

                    val scrollPane = JBScrollPane(textArea).apply {
                        setBorder(JBUI.Borders.empty())
                    }

                    val copyButton = JButton(AllIcons.Actions.Copy).apply {
                        toolTipText = "Copy to clipboard"
                        preferredSize = Dimension(30, 30)
                        isOpaque = false
                        isContentAreaFilled = false
                        isBorderPainted = false
                        addActionListener {
                            CopyPasteManager.getInstance().setContents(StringSelection(textArea.text))
                        }
                    }

                    val buttonPanel = JPanel(BorderLayout()).apply {
                        isOpaque = false
                        add(Box.createHorizontalGlue(), BorderLayout.CENTER)
                        add(copyButton, BorderLayout.EAST)
                        setBorder(JBUI.Borders.emptyBottom(5))
                    }

                    add(buttonPanel, BorderLayout.NORTH)
                    add(scrollPane, BorderLayout.CENTER)
                })
                    .align(AlignX.FILL + AlignY.FILL)
                    .resizableColumn()
            }.resizableRow()
        }.apply {
            minimumSize = Dimension(600, 200)
        }
    }

    override fun createActions(): Array<Action> {
        val actions: Array<Action> = super.createActions()
        for (action in actions) {
            if ("OK" == action.getValue(Action.NAME)?.toString()) {
                action.putValue(Action.NAME, "Run")
                runAction = action
                break
            }
        }
        return actions
    }

    override fun createSouthPanel(): JComponent? {
        val southPanel = super.createSouthPanel() ?: return null

        val verboseCheckBox = JCheckBox("Enable verbose logging").apply {
            isSelected = enableVerboseLogging
            toolTipText = "Show detailed PITest output in console"
            addActionListener {
                enableVerboseLogging = isSelected
            }
        }

        val panel = JPanel(BorderLayout())
        panel.add(verboseCheckBox, BorderLayout.WEST)
        panel.add(southPanel, BorderLayout.EAST)

        return panel
    }

    private fun refreshDialog() {
        val currentSize = size
        val currentWidth = currentSize.width
        val currentHeight = currentSize.height
        val contentPane = contentPanel
        contentPane.removeAll()
        mainPanel = createMainPanel()
        contentPane.add(mainPanel!!, BorderLayout.CENTER)
        contentPane.revalidate()
        contentPane.repaint()
        if (currentWidth > 0 && currentHeight > 0) {
            setSize(currentWidth, currentHeight)
        } else {
            pack()
        }
        setMinimumSize()
    }

    private fun createModifyOptionsPopup(): JBPopup {
        val options = createModifyOptionsList()
        val step = object : BaseListPopupStep<ModifyOption>("Modify Options", options) {
            override fun isSelectable(value: ModifyOption?): Boolean = true

            override fun onChosen(selectedValue: ModifyOption?, finalChoice: Boolean): PopupStep<*>? {
                if (selectedValue != null) {
                    val newState = !selectedValue.isSelected()
                    selectedValue.onToggle(newState)
                    refreshDialog()
                }
                return null
            }

            override fun hasSubstep(selectedValue: ModifyOption?): Boolean = false

            override fun getTextFor(value: ModifyOption?): String = value?.text ?: ""
            fun isSelected(value: ModifyOption?): Boolean = value?.isSelected() ?: false
        }

        return JBPopupFactory.getInstance()
            .createListPopup(step)
            .apply {
                setTitle("Modify Options")
            }
    }

    private fun createModifyOptionsList(): List<ModifyOption> {
        return listOf(
            ModifyOption(
                if (buildSystem == BuildSystem.MAVEN) "Pre goals" else "Pre tasks",
                { showPreGoals },
                { showPreGoals = it }
            ),
            ModifyOption(
                if (buildSystem == BuildSystem.MAVEN) "Post goals" else "Post tasks",
                { showPostGoals },
                { showPostGoals = it }
            )
        )
    }

    private fun getBuildUnitDisplayName(): String {
        return selectedBuildUnit?.let { buildUnit ->
            if (buildUnit.parent != null) {
                val parentName = buildUnit.parent.buildPath.parent.fileName?.toString() ?: "parent"
                val childName = buildUnit.buildPath.parent.fileName?.toString() ?: "moduleName"
                "$parentName.$childName"
            } else {
                buildUnit.buildPath.parent.fileName?.toString() ?: "Project"
            }
        } ?: "Project"
    }

    private fun getHierarchicalBuildUnits(): List<BuildUnit> {
        val result = mutableListOf<BuildUnit>()
        val added = mutableSetOf<Path>()

        val rootBuildUnits = buildUnits.filter { it.parent == null }

        fun addBuildUnitAndChildren(buildUnit: BuildUnit) {
            if (buildUnit.buildPath !in added) {
                result.add(buildUnit)
                added.add(buildUnit.buildPath)
            }
            buildUnit.children.forEach { child ->
                addBuildUnitAndChildren(child)
            }
        }

        rootBuildUnits.forEach { root ->
            addBuildUnitAndChildren(root)
        }

        buildUnits.filter { it.buildPath !in added }.forEach { buildUnit ->
            result.add(buildUnit)
            added.add(buildUnit.buildPath)
        }

        return result
    }

    private fun createBuildUnitPopup(): JBPopup {
        val hierarchicalBuildUnits = getHierarchicalBuildUnits()

        val step = object : BaseListPopupStep<BuildUnit>("Project", hierarchicalBuildUnits) {
            override fun isSelectable(value: BuildUnit?): Boolean = true

            override fun onChosen(selectedValue: BuildUnit?, finalChoice: Boolean): PopupStep<*>? {
                if (selectedValue != null) {
                    selectedBuildUnit = selectedValue
                    refreshDialog()
                }
                return null
            }

            override fun hasSubstep(selectedValue: BuildUnit?): Boolean = false

            override fun getTextFor(value: BuildUnit?): String {
                return value?.let { buildUnit ->
                    if (buildUnit.parent != null) {
                        val parentName = buildUnit.parent.buildPath.parent.fileName?.toString() ?: "parent"
                        val childName = buildUnit.buildPath.parent.fileName?.toString() ?: "moduleName"
                        "$parentName.$childName"
                    } else {
                        buildUnit.buildPath.parent.fileName?.toString() ?: "Project"
                    }
                } ?: "Project"
            }

            override fun getIconFor(value: BuildUnit?): javax.swing.Icon? {
                return AllIcons.Nodes.Module
            }

            fun isSelected(value: BuildUnit?): Boolean {
                return value == selectedBuildUnit
            }
        }

        return JBPopupFactory.getInstance()
            .createListPopup(step)
            .apply {
                setTitle("Project")
            }
    }

    private fun buildCommand() = mutationCoverageOptions.buildCommand(buildSystem)

    private fun updateCommandTextArea() {
        commandTextArea.text = buildCommand()
    }
}

