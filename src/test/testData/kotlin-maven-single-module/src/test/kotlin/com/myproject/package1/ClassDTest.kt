package com.myproject.package1

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClassDTest {

    @Test
    fun testStringSelector() {
        val classD = ClassD()
        assertEquals(classD.getNumber("Three"), 3)
    }
}