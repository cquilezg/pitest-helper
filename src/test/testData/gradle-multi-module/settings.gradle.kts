plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "gradle-multi-module"

include("app")
include("lib") // Add lib to the build