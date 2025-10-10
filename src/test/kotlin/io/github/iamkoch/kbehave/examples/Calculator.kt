package io.github.iamkoch.kbehave.examples

/**
 * Simple calculator for demonstration purposes.
 */
class Calculator {
    fun add(x: Int, y: Int): Int = x + y
    fun subtract(x: Int, y: Int): Int = x - y
    fun multiply(x: Int, y: Int): Int = x * y
    fun divide(x: Int, y: Int): Int {
        if (y == 0) throw IllegalArgumentException("Division by zero")
        return x / y
    }
}
