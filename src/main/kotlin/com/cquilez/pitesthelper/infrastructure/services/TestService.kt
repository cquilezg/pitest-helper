package com.cquilez.pitesthelper.services

object TestService {
    fun getClassUnderTestName(testClassName: String): String = testClassName.removeSuffix("Test")
}
