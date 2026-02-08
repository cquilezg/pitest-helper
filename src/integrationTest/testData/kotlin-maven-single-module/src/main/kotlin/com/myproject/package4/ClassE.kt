package com.myproject.package4

class ClassE {
    fun getNumber(string: String): Int {
        return when (string) {
            "One" -> 1
            "Two" -> 2
            "Three" -> 3
            else -> -1
        }
    }
}