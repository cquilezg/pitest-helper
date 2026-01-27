package com.cquilez.pitesthelper.ui

/**
 * Annotation to mark a field that should receive the BackgroundRun instance
 * created by the UiTestExtension.
 *
 * @param testName The name used to identify this test run (e.g., "javaMavenUiTest")
 * @param projectPath The path to the test project relative to the test data directory
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class IDEInstance(
    val testName: String,
    val projectPath: String
)
