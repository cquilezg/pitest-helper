package com.cquilez.pitesthelper.application.cqrs.command

import java.nio.file.Path

data class RunMutationCoverageFromProjectViewCommand(val nodes: List<Path>)
