package com.myproject.package3

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClassATest {

    @Test
    fun testNumberSelector() {
        val classA = ClassA()
        assertEquals(classA.getNumber(2), "Two")
    }
}