package com.cquilez.pitesthelper.application.port.`in`

import com.cquilez.pitesthelper.application.cqrs.command.RunMutationCoverageFromProjectViewCommand

fun interface RunMutationCoverageFromProjectViewPort {
    fun execute(command: RunMutationCoverageFromProjectViewCommand)
}