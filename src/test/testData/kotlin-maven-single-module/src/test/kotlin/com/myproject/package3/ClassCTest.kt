package com.myproject.package3

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClassCTest {

    @Test
    fun testStringSelector() {
        val classC = ClassC()
        assertEquals(classC.getNumber("Three"), 3)
    }
}