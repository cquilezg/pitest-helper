package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.model.MutationCoverageData

interface UserInterfaceOutPort {
    fun showMutationCoverageDialog(mutationCoverageData: MutationCoverageData): Boolean
}