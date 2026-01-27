package com.myproject.package1

class ClassA {
    fun getNumber(number: Int): String {
        return when (number) {
            1 -> "One"
            2 -> "Two"
            3 -> "Three"
            else -> "Unknown"
        }
    }
}