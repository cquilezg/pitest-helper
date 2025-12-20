package com.example.myapplication

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class CalculatorTest {
    private lateinit var calculator: Calculator

    @Before
    fun setUp() {
        calculator = Calculator()
    }

    @Test
    fun testAdd() {
        assertEquals(5, calculator.add(2, 3))
    }

    @Test
    fun testSubtract() {
        assertEquals(1, calculator.subtract(3, 2))
    }

    @Test
    fun testMultiply() {
        assertEquals(6, calculator.multiply(2, 3))
    }

    @Test
    fun testDivide() {
        assertEquals(2, calculator.divide(6, 3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDivideByZero() {
        calculator.divide(5, 0)
    }

    @Test
    fun testIsPositive() {
        assertTrue(calculator.isPositive(5))
        assertFalse(calculator.isPositive(-5))
        assertFalse(calculator.isPositive(0))
    }
}