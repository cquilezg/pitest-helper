plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "gradle-multi-module"

include("app")
include("lib") // Add lib to the build