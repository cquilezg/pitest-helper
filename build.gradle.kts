import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.kotlin) // Kotlin support (includes Java plugin)
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

// Configure project's dependencies
repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

// Extend classpaths to include IntelliJ Platform dependencies
configurations["integrationTestCompileClasspath"].extendsFrom(configurations["testCompileClasspath"])
configurations["integrationTestRuntimeClasspath"].extendsFrom(configurations["testRuntimeClasspath"])

// Extend from IntelliJ Platform test framework configuration (for ide-starter-squashed)
afterEvaluate {
    configurations.matching { it.name.startsWith("intellijPlatform") && it.name.contains("test", ignoreCase = true) }
        .forEach { config ->
            configurations["integrationTestCompileClasspath"].extendsFrom(config)
            configurations["integrationTestRuntimeClasspath"].extendsFrom(config)
        }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiterParams)
    testImplementation(libs.junitJupiterSuite)
    testImplementation(libs.hamcrest)
    testImplementation(libs.kotlinTest)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junitJupiterEngine)

    // Additional dependencies for integration tests (inherits testImplementation via extendsFrom)
    integrationTestImplementation(libs.kodeinDi)
    integrationTestImplementation(libs.kotlinxCoroutines)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Starter)
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion")
            .map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            create("IC", "2025.2.6")
            create("IC", "2024.3.7")
            create("IC", "2023.3.8")
            create("IC", "2022.3.3")
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

// JVM arguments required for IntelliJ Platform test framework (Java 17+ module access)
val javaModuleOpenArgs = listOf(
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
    "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
    "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
    "--add-opens=java.desktop/sun.font=ALL-UNNAMED"
)

tasks {
    test {
        useJUnitPlatform()
    }

    register<Test>("integrationTest") {
        // Mark as not compatible with configuration cache due to dynamic file operations
        notCompatibleWithConfigurationCache("Uses dynamic file discovery for plugin and IDE paths")

        dependsOn("buildPlugin")

        val integrationTestSourceSet = sourceSets.getByName("integrationTest")
        testClassesDirs = integrationTestSourceSet.output.classesDirs
        classpath = integrationTestSourceSet.runtimeClasspath

        // Disable IntelliJ's JUnit 5 test environment initializer (not needed for Starter-based tests)
        systemProperty("junit.platform.launcher.interceptors.enabled", "false")
        useJUnitPlatform {
            val tags = project.findProperty("tags")
            if (tags != null) {
                includeTags(tags.toString())
            }
            val excludedtags = project.findProperty("excludeTags")
            if (excludedtags != null) {
                excludeTags = setOf(excludedtags.toString())
            }
        }
        jvmArgs(javaModuleOpenArgs)
        
        // Set system properties at execution time
        doFirst {
            // Find the plugin zip in the build directory
            val distributionsDir = layout.buildDirectory.dir("distributions").get().asFile
            val pluginZip = distributionsDir.listFiles()?.firstOrNull { it.name.endsWith(".zip") }
                ?: error("Plugin zip not found in $distributionsDir")
            systemProperty("path.to.build.plugin", pluginZip.absolutePath)

            // Workaround for Starter framework PathManager warning (https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1997)
            // The Driver SDK runs in the Gradle test JVM and uses IntelliJ logging APIs that expect an IDE installation.
            // We dynamically find the downloaded IDE path at test execution time.
            val ideTestsCache = file("out/ide-tests/cache/builds")
            if (ideTestsCache.exists()) {
                val ideDir = ideTestsCache.listFiles()
                    ?.filter { it.isDirectory && it.name.startsWith("IC-") }
                    ?.maxByOrNull { it.lastModified() }
                    ?.listFiles()
                    ?.firstOrNull { it.isDirectory && it.name.startsWith("idea-IC-") }

                if (ideDir != null) {
                    systemProperty("idea.home.path", ideDir.absolutePath)
                }
            }
        }
    }

    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}