package com.myproject.package2

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClassBTest {

    @Test
    fun testNumberSelector() {
        assertEquals(ClassB.getNumber(2), "Two")
    }
}