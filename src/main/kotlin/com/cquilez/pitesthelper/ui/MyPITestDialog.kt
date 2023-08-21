package com.cquilez.pitesthelper.ui

import com.cquilez.pitesthelper.services.MavenService
import com.intellij.openapi.module.Module
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*

class MyPITestDialog(private val project: Project, private val module: Module) : DialogWrapper(true) {
    private val tfTargetClasses = JTextField()
    private val tfTargetTests = JTextField()
    private val commandTextArea = JTextArea(5, 30)
    private val publicarCheckBox = JCheckBox("Publicar")
    private val cargarCheckBox = JCheckBox("Cargar")
    var targetClasses: String? = ""
        set(value) {
            tfTargetClasses.text = value
            field = value
        }
    var targetTests: String? = ""
        set(value) {
            tfTargetTests.text = value
            field = value
        }

    init {
        title = "Mutation Coverage"
        init()
        setSize(
            Toolkit.getDefaultToolkit().screenSize.width / 2,
            size.height
        )
        pack()
        centerRelativeToParent()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        panel.minimumSize = Dimension(600, 200)
        val gbConstraints = GridBagConstraints()

        gbConstraints.gridx = 0
        gbConstraints.gridy = 0
        gbConstraints.weightx = 0.0
        gbConstraints.fill = GridBagConstraints.NONE
        panel.add(JLabel("Target Classes"), gbConstraints)

        gbConstraints.gridx++
        gbConstraints.weightx = 1.0
        gbConstraints.fill = GridBagConstraints.HORIZONTAL
        panel.add(tfTargetClasses, gbConstraints)

        gbConstraints.gridx = 0
        gbConstraints.gridy++
        gbConstraints.weightx = 0.0
        gbConstraints.fill = GridBagConstraints.NONE
        panel.add(JLabel("Target Tests"), gbConstraints)

        gbConstraints.gridx++
        gbConstraints.weightx = 1.0
        gbConstraints.fill = GridBagConstraints.HORIZONTAL
        panel.add(tfTargetTests, gbConstraints)

        gbConstraints.gridx = 0
        gbConstraints.gridy++
        gbConstraints.weightx = 0.0
        gbConstraints.fill = GridBagConstraints.NONE
        panel.add(JLabel("Maven command "), gbConstraints)

        gbConstraints.gridx++
        gbConstraints.weightx = 1.0
        gbConstraints.weighty = 1.0
        gbConstraints.fill = GridBagConstraints.BOTH

        val scrollPane = JBScrollPane(commandTextArea)
        commandTextArea.isEditable = false
        commandTextArea.lineWrap = true
        commandTextArea.margin = JBUI.insets(10)
        commandTextArea.minimumSize = Dimension()
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        panel.add(scrollPane, gbConstraints)

        gbConstraints.gridx = 0
        gbConstraints.gridy++
        gbConstraints.weightx = 0.0
        gbConstraints.weighty = 0.0
        gbConstraints.fill = GridBagConstraints.NONE
        panel.add(publicarCheckBox, gbConstraints)

        gbConstraints.gridx++
        gbConstraints.anchor = GridBagConstraints.LINE_START
        panel.add(cargarCheckBox, gbConstraints)

        tfTargetClasses.whenTextChanged { buildMavenCommand() }
        tfTargetTests.whenTextChanged { buildMavenCommand() }

        return panel
    }

    override fun doOKAction() {
        MavenService.runMavenCommand(project, module, listOf("test-compile", "pitest:mutationCoverage"), MavenService.buildPitestArgs(tfTargetClasses.text, tfTargetTests.text))
        super.doOKAction()
    }

    private fun buildMavenCommand() {
        commandTextArea.text = "mvn test-compile pitest:mutationCoverage ${MavenService.buildPitestArgs(tfTargetClasses.text, tfTargetTests.text)}"
    }
}
