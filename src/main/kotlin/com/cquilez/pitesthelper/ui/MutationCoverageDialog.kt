package com.cquilez.pitesthelper.ui

import com.cquilez.pitesthelper.model.MutationCoverageCommandData
import com.cquilez.pitesthelper.model.MutationCoverageData
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Toolkit
import javax.swing.Action
import javax.swing.JComponent

class MutationCoverageDialog(mutationCoverageData: MutationCoverageData, private val commandBuilder: java.util.function.Function<MutationCoverageCommandData, String>) : DialogWrapper(true) {
    private val commandTextArea = JBTextArea()

    var commandData = MutationCoverageCommandData(
        mutationCoverageData.module,
        mutationCoverageData.targetClasses.joinToString(","),
        mutationCoverageData.targetTests.joinToString(",")
    )

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
            row("Target Classes:") {
                textField()
                    .align(AlignX.FILL)
                    .bindText(commandData::targetClasses)
                    .applyToComponent {
                        document.whenTextChanged {
                            commandData.targetClasses = text.replace(" ", "")
                            updateCommandTextArea()
                        }
                    }
            }
            row("Target Tests:") {
                textField()
                    .align(AlignX.FILL)
                    .bindText(commandData::targetTests)
                    .applyToComponent {
                        document.whenTextChanged {
                            commandData.targetTests = text.replace(" ", "")
                            updateCommandTextArea()
                        }
                    }
            }
            row("Run command:") {
                scrollCell(commandTextArea)
                    .text(buildCommand())
                    .rows(5).columns(30)
                    .align(AlignX.FILL + AlignY.FILL)
                    .applyToComponent {
                        isEditable = false
                        lineWrap = true
                        margin = JBUI.insets(10)
                        minimumSize = Dimension()
                        resizableRow()
                    }
            }
        }.apply() {
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

    private fun buildCommand() = commandBuilder.apply(commandData)

    private fun updateCommandTextArea() {
        commandTextArea.text = buildCommand()
    }
}
