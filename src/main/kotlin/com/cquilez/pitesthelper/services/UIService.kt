package com.cquilez.pitesthelper.services

import com.cquilez.pitesthelper.application.port.out.UserInterfaceOutPort
import com.cquilez.pitesthelper.model.MutationCoverageData
import com.cquilez.pitesthelper.ui.MutationCoverageDialog
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

/**
 * Manages UI
 */
@Service
class UIService : UserInterfaceOutPort {

    /**
     * Show dialog only if unit test mode is disabled or execute orElseAction
     */
    fun showDialog(mainAction: Runnable, orElseAction: Runnable) {
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            mainAction.run()
        } else {
            orElseAction.run()
        }
    }

    override fun showMutationCoverageDialog(mutationCoverageData: MutationCoverageData) : Boolean {
        val dialog = MutationCoverageDialog(mutationCoverageData, processor::buildCommand)
        dialog.show()
        if (dialog.isOK) {
            processor.runCommand(dialog.commandData)
        }
    }
}
