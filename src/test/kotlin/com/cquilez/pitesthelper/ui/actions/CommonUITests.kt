package com.cquilez.pitesthelper.ui.actions

import com.cquilez.pitesthelper.ui.steps.SharedSteps
import com.intellij.remoterobot.RemoteRobot

class CommonUITests {

    class SingleNodeTest {
        companion object {
            fun singleMainClass_testClassExists_singleMainClassAndSingleTestClass(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot, listOf("src", "main", "java", "com.myproject", "package1", "ClassA"),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.ClassA " +
                            "$targetTests=com.myproject.package1.ClassATest",
                    false
                )

            fun singleMainPackage_testPackageExists_singleMainPackageAndSingleTestPackage(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) = SharedSteps.runMutationCoverage(
                testProject, remoteRobot, listOf("src", "main", "java", "com.myproject", "package2"),
                "$buildCommand " +
                        "$targetClasses=com.myproject.package2.* " +
                        "$targetTests=com.myproject.package2.*",
                false
            )

            fun singleTestClass_mainClassExists_singleTargetClassAndSingleTestClass(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot, listOf("src", "test", "java", "com.myproject", "package2", "ClassBTest"),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package2.ClassB " +
                            "$targetTests=com.myproject.package2.ClassBTest",
                    false
                )

            fun singleTestPackage_mainPackageExists_singleTargetPackageAndSingleTestPackage(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) = SharedSteps.runMutationCoverage(
                testProject, remoteRobot, listOf("src", "test", "java", "com.myproject", "package1"),
                "$buildCommand " +
                        "$targetClasses=com.myproject.package1.* " +
                        "$targetTests=com.myproject.package1.*",
                false
            )
        }
    }

    class MultiNodeTest {
        companion object {
            fun twoMainClassesSelected_testClassesExists_TwoTargetClassesAndTwoTestClasses(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot,
                    setOf(
                        listOf("src", "main", "java", "com.myproject", "package1", "ClassA"),
                        listOf("src", "main", "java", "com.myproject", "package1", "ClassD")
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.ClassA,com.myproject.package1.ClassD " +
                            "$targetTests=com.myproject.package1.ClassATest,com.myproject.package1.ClassDTest",
                    false
                )

            fun twoTestClassesSelected_mainClassesExists_twoTargetClassesAndTwoTestClasses(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot,
                    setOf(
                        listOf("src", "test", "java", "com.myproject", "package1", "ClassATest"),
                        listOf("src", "test", "java", "com.myproject", "package2", "ClassBTest")
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.ClassA,com.myproject.package2.ClassB " +
                            "$targetTests=com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest",
                    false
                )

            fun twoMainPackages_testPackagesExists_twoMainPackagesAndTwoTestPackages(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot,
                    setOf(
                        listOf("src", "main", "java", "com.myproject", "package1"),
                        listOf("src", "main", "java", "com.myproject", "package2")
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.*,com.myproject.package2.* " +
                            "$targetTests=com.myproject.package1.*,com.myproject.package2.*",
                    false
                )

            fun twoTestPackages_mainPackagesExists_TwoMainPackagesAndTwoTestPackages(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot,
                    setOf(
                        listOf("src", "test", "java", "com.myproject", "package1"),
                        listOf("src", "test", "java", "com.myproject", "package2")
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.*,com.myproject.package2.* " +
                            "$targetTests=com.myproject.package1.*,com.myproject.package2.*",
                    false
                )

            fun mainClassAndItsPackage_testPackageExists_onlyPackagesAreSelected(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot,
                    setOf(
                        listOf("src", "main", "java", "com.myproject", "package1", "ClassA"),
                        listOf("src", "main", "java", "com.myproject", "package1")
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.* " +
                            "$targetTests=com.myproject.package1.*",
                    false
                )

            fun testClassAndParentPackage_mainPackageExists_onlyPackageSelected(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot,
                    setOf(
                        listOf("src", "test", "java", "com.myproject", "package2", "ClassBTest"),
                        listOf("src", "test", "java", "com.myproject", "package2")
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package2.* " +
                            "$targetTests=com.myproject.package2.*",
                    false
                )
        }
    }

    class CrossSourceTest {
        companion object {
            fun mainClassAndItsTestClass_bothClassesAreSelected(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot, setOf(
                        listOf("src", "main", "java", "com.myproject", "package1", "ClassA"),
                        listOf("src", "test", "java", "com.myproject", "package1", "ClassATest")
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.ClassA " +
                            "$targetTests=com.myproject.package1.ClassATest",
                    false
                )

            fun mainClassAndDifferentTestClass_mainClassesAndTestClassesSelected(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) = SharedSteps.runMutationCoverage(
                testProject, remoteRobot, setOf(
                    listOf("src", "main", "java", "com.myproject", "package1", "ClassA"),
                    listOf("src", "test", "java", "com.myproject", "package2", "ClassBTest")
                ),
                "$buildCommand " +
                        "$targetClasses=com.myproject.package1.ClassA,com.myproject.package2.ClassB " +
                        "$targetTests=com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest",
                false
            )
        }
    }

    class SpecialCasesTest {
        companion object {
            fun mainClass_multipleTestClassCandidatesAndOneInSamePackage_testClassInSamePackage(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot, listOf("src", "main", "java", "com.myproject", "package1", "ClassC"),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.ClassC " +
                            "$targetTests=com.myproject.package1.ClassCTest",
                    false
                )

            fun mainClass_multipleTestClassCandidatesAndOneInASuperiorPackage_testClassInSuperiorPackage(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) = SharedSteps.runMutationCoverage(
                testProject, remoteRobot, listOf("src", "main", "java", "com.myproject", "package3.impl", "ClassC"),
                "$buildCommand " +
                        "$targetClasses=com.myproject.package3.impl.ClassC " +
                        "$targetTests=com.myproject.package3.ClassCTest",
                false
            )

            fun testClass_singleMainClassCandidateInASuperiorPackage_mainClassFound(
                testProject: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) = SharedSteps.runMutationCoverage(
                testProject, remoteRobot, listOf("src", "test", "java", "com.myproject", "package4.impl", "ClassETest"),
                "$buildCommand " +
                        "$targetClasses=com.myproject.package4.ClassE " +
                        "$targetTests=com.myproject.package4.impl.ClassETest",
                false
            )
        }
    }

}