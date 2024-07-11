//package ca.ubc.ece.resess.util
//
//import junit.framework.TestCase
//
//class SourceLocationTest : TestCase() {
//    fun testToString() {
//        assertEquals("Main:3", SourceLocation("Main", 3).toString())
//    }
//
//    fun testEqualsTrue() {
//        assertTrue(SourceLocation("Main", 3) == SourceLocation("Main", 3))
//    }
//
//    fun testEqualsFalseOne() {
//        assertFalse(SourceLocation("Main", 0) == SourceLocation("Main", 3))
//    }
//
//    fun testEqualsFalseTwo() {
//        assertFalse(SourceLocation("Main", 3) == SourceLocation("Main2", 3))
//    }
//
//    fun testHashCode() {
//        assertEquals(SourceLocation("Main", 3).hashCode(), SourceLocation("Main", 3).hashCode())
//    }
//}