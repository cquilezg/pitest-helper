package com.myproject.package1

class ClassC {
    fun getNumber(string: String): Int {
        return when (string) {
            "One" -> 1
            "Two" -> 2
            "Three" -> 3
            else -> -1
        }
    }
}