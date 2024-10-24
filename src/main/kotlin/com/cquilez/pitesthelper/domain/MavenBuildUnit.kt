package com.cquilez.pitesthelper.domain

import java.nio.file.Path

class MavenBuildUnit(buildPath2: Path, buildFileName2: String) : BuildUnit(BuildSystem.MAVEN, buildPath2, buildFileName2) {
}