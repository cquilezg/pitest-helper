package com.cquilez.pitesthelper.application.port.out

import com.cquilez.pitesthelper.domain.MutationCoverageOptions

fun interface UserInterfacePort {
    fun showMutationCoverageDialog(options: MutationCoverageOptions)
}