package com.cquilez.pitesthelper.domain

import java.nio.file.Path

open class BuildUnit(val buildSystem: BuildSystem, open val buildPath: Path, val buildFileName: String)
