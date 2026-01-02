package com.cquilez.pitesthelper.infrastructure.ui

import com.cquilez.pitesthelper.application.port.out.BuildSystemPort
import com.cquilez.pitesthelper.domain.MutationCoverageOptions
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Toolkit
import javax.swing.Action
import javax.swing.JComponent

class MutationCoverageDialog(
    val mutationCoverageOptions: MutationCoverageOptions,
    private val buildSystemPort: BuildSystemPort
) : DialogWrapper(true) {
    private val commandTextArea = JBTextArea()
    private var targetClasses = ""
    private var targetTests = ""

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
                ).align(AlignX.RIGHT)
            }
            row("Target Classes:") {
                textField()
                    .align(AlignX.FILL)
                    .bindText(mutationCoverageOptions::targetClasses)
                    .applyToComponent {
                        document.whenTextChanged {
                            targetClasses = text.replace(" ", "")
                            updateCommandTextArea()
                        }
                    }
            }
            row("Target Tests:") {
                textField()
                    .align(AlignX.FILL)
                    .bindText(mutationCoverageOptions::targetTests)
                    .applyToComponent {
                        document.whenTextChanged {
                            targetTests = text.replace(" ", "")
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

    private fun buildCommand() = buildSystemPort.buildCommand(mutationCoverageOptions)

    private fun updateCommandTextArea() {
        commandTextArea.text = buildCommand()
    }
}
