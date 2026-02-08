package com.myproject.package4.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClassETest {

    @Test
    fun testStringSelector() {
        val classE = ClassE()
        assertEquals(classE.getNumber("Three"), 3)
    }
}