package com.cquilez.pitesthelper.ui.actions

import com.cquilez.pitesthelper.ui.steps.SharedSteps
import com.intellij.remoterobot.RemoteRobot

class CommonUITests {

    companion object {
        private fun buildNodeList(module: String?, mutableNodeList: MutableList<String>): List<String> {
            if (module?.isNotBlank() == true) {
                mutableNodeList.add(0, module)
            }
            return mutableNodeList
        }
    }

    class SingleNodeTest {
        companion object {
            fun singleMainClass_testClassExists_singleMainClassAndSingleTestClass(
                testProject: String,
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject,
                    remoteRobot,
                    buildNodeList(
                        testModule,
                        mutableListOf("src", "main", language, "com.myproject", "package1", "ClassA")
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.ClassA " +
                            "$targetTests=com.myproject.package1.ClassATest",
                    false
                )

            fun singleMainPackage_testPackageExists_singleMainPackageAndSingleTestPackage(
                testProject: String,
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) = SharedSteps.runMutationCoverage(
                testProject,
                remoteRobot,
                buildNodeList(testModule, mutableListOf("src", "main", language, "com.myproject", "package2")),
                "$buildCommand " +
                        "$targetClasses=com.myproject.package2.* " +
                        "$targetTests=com.myproject.package2.*",
                false
            )

            fun singleTestClass_mainClassExists_singleTargetClassAndSingleTestClass(
                testProject: String,
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject,
                    remoteRobot,
                    buildNodeList(
                        testModule,
                        mutableListOf("src", "test", language, "com.myproject", "package2", "ClassBTest")
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package2.ClassB " +
                            "$targetTests=com.myproject.package2.ClassBTest",
                    false
                )

            fun singleTestPackage_mainPackageExists_singleTargetPackageAndSingleTestPackage(
                testProject: String,
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) = SharedSteps.runMutationCoverage(
                testProject,
                remoteRobot,
                buildNodeList(testModule, mutableListOf("src", "test", language, "com.myproject", "package1")),
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
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot,
                    setOf(
                        buildNodeList(
                            testModule,
                            mutableListOf("src", "main", language, "com.myproject", "package1", "ClassA")
                        ),
                        buildNodeList(
                            testModule,
                            mutableListOf("src", "main", language, "com.myproject", "package1", "ClassD")
                        )
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.ClassA,com.myproject.package1.ClassD " +
                            "$targetTests=com.myproject.package1.ClassATest,com.myproject.package1.ClassDTest",
                    false
                )

            fun twoTestClassesSelected_mainClassesExists_twoTargetClassesAndTwoTestClasses(
                testProject: String,
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot,
                    setOf(
                        buildNodeList(
                            testModule,
                            mutableListOf("src", "test", language, "com.myproject", "package1", "ClassATest")
                        ),
                        buildNodeList(
                            testModule,
                            mutableListOf("src", "test", language, "com.myproject", "package2", "ClassBTest")
                        )
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.ClassA,com.myproject.package2.ClassB " +
                            "$targetTests=com.myproject.package1.ClassATest,com.myproject.package2.ClassBTest",
                    false
                )

            fun twoMainPackages_testPackagesExists_twoMainPackagesAndTwoTestPackages(
                testProject: String,
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot,
                    setOf(
                        buildNodeList(testModule, mutableListOf("src", "main", language, "com.myproject", "package1")),
                        buildNodeList(testModule, mutableListOf("src", "main", language, "com.myproject", "package2"))
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.*,com.myproject.package2.* " +
                            "$targetTests=com.myproject.package1.*,com.myproject.package2.*",
                    false
                )

            fun twoTestPackages_mainPackagesExists_TwoMainPackagesAndTwoTestPackages(
                testProject: String,
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot,
                    setOf(
                        buildNodeList(testModule, mutableListOf("src", "test", language, "com.myproject", "package1")),
                        buildNodeList(testModule, mutableListOf("src", "test", language, "com.myproject", "package2"))
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.*,com.myproject.package2.* " +
                            "$targetTests=com.myproject.package1.*,com.myproject.package2.*",
                    false
                )

            fun mainClassAndItsPackage_testPackageExists_onlyPackagesAreSelected(
                testProject: String,
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot,
                    setOf(
                        buildNodeList(
                            testModule,
                            mutableListOf("src", "main", language, "com.myproject", "package1", "ClassA")
                        ),
                        buildNodeList(testModule, mutableListOf("src", "main", language, "com.myproject", "package1"))
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.* " +
                            "$targetTests=com.myproject.package1.*",
                    false
                )

            fun testClassAndParentPackage_mainPackageExists_onlyPackageSelected(
                testProject: String,
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot,
                    setOf(
                        buildNodeList(
                            testModule,
                            mutableListOf("src", "test", language, "com.myproject", "package2", "ClassBTest")
                        ),
                        buildNodeList(testModule, mutableListOf("src", "test", language, "com.myproject", "package2"))
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
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject, remoteRobot, setOf(
                        buildNodeList(
                            testModule,
                            mutableListOf("src", "main", language, "com.myproject", "package1", "ClassA")
                        ),
                        buildNodeList(
                            testModule,
                            mutableListOf("src", "test", language, "com.myproject", "package1", "ClassATest")
                        )
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.ClassA " +
                            "$targetTests=com.myproject.package1.ClassATest",
                    false
                )

            fun mainClassAndDifferentTestClass_mainClassesAndTestClassesSelected(
                testProject: String,
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) = SharedSteps.runMutationCoverage(
                testProject, remoteRobot, setOf(
                    buildNodeList(
                        testModule,
                        mutableListOf("src", "main", language, "com.myproject", "package1", "ClassA")
                    ),
                    buildNodeList(
                        testModule,
                        mutableListOf("src", "test", language, "com.myproject", "package2", "ClassBTest")
                    )
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
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) =
                SharedSteps.runMutationCoverage(
                    testProject,
                    remoteRobot,
                    buildNodeList(
                        testModule,
                        mutableListOf("src", "main", language, "com.myproject", "package1", "ClassC")
                    ),
                    "$buildCommand " +
                            "$targetClasses=com.myproject.package1.ClassC " +
                            "$targetTests=com.myproject.package1.ClassCTest",
                    false
                )

            fun mainClass_multipleTestClassCandidatesAndOneInASuperiorPackage_testClassInSuperiorPackage(
                testProject: String,
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) = SharedSteps.runMutationCoverage(
                testProject,
                remoteRobot,
                buildNodeList(
                    testModule,
                    mutableListOf("src", "main", language, "com.myproject", "package3.impl", "ClassC")
                ),
                "$buildCommand " +
                        "$targetClasses=com.myproject.package3.impl.ClassC " +
                        "$targetTests=com.myproject.package3.ClassCTest",
                false
            )

            fun testClass_singleMainClassCandidateInASuperiorPackage_mainClassFound(
                testProject: String,
                testModule: String?,
                language: String,
                buildCommand: String,
                targetClasses: String,
                targetTests: String,
                remoteRobot: RemoteRobot
            ) = SharedSteps.runMutationCoverage(
                testProject,
                remoteRobot,
                buildNodeList(
                    testModule,
                    mutableListOf("src", "test", language, "com.myproject", "package4.impl", "ClassETest")
                ),
                "$buildCommand " +
                        "$targetClasses=com.myproject.package4.ClassE " +
                        "$targetTests=com.myproject.package4.impl.ClassETest",
                false
            )
        }
    }

}