package com.myproject.package2

object ClassB {
    fun getNumber(number: Int): String {
        return when (number) {
            1 -> "One"
            2 -> "Two"
            3 -> "Three"
            else -> "Unknown"
        }
    }
}