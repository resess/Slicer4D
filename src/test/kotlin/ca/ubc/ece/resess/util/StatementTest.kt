package ca.ubc.ece.resess.util

import junit.framework.TestCase

class StatementTest : TestCase() {
    fun testToString() {
        assertEquals("Main:3", Statement("Main", 3).toString())
    }

    fun testEqualsTrue() {
        assertTrue(Statement("Main", 3) == Statement("Main", 3))
    }

    fun testEqualsFalseOne() {
        assertFalse(Statement("Main", 0) == Statement("Main", 3))
    }

    fun testEqualsFalseTwo() {
        assertFalse(Statement("Main", 3) == Statement("Main2", 3))
    }

    fun testHashCode() {
        assertEquals(
            Statement("Main", 3).hashCode(), Statement("Main", 3).hashCode())
    }
}