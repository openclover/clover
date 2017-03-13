package com.atlassian.clover.remote

import org.junit.Test

import static org.junit.Assert.*

class InitStringDataTest {

    @Test
    void testEmptyInitObject() {
        InitStringData data = new InitStringData("")
        assertEquals("", data.toString())
    }

    @Test
    void testParseInitStringData() {
        InitStringData data = new InitStringData("key1=value1; key2 =  Value Two;key3=value3")
        assertEquals("value1", data.get("key1"))
        assertEquals("Value Two", data.get("key2"))
        assertEquals("value3", data.get("key3"))

        data = new InitStringData(";key=value")
        assertEquals("value", data.get("key"))
    }

    @Test
    void testGet() {
        InitStringData data = new InitStringData("intVal=2;strVal=testString")
        assertEquals(2, data.get("intVal", 0))
        assertEquals("testString", data.get("strVal", ""))
        assertEquals(3, data.get("NonExistantInt", 3))
        assertEquals("defaultValue", data.get("NonExistantString", "defaultValue"))
    }

    @Test
    void testGetBadInt() {
        InitStringData data = new InitStringData("intVal=NaN")
        assertEquals(3, data.get("intVal", 3))
    }

    @Test
    void testInvalidInitString() {
        try {
            new InitStringData("intVal=")
            fail("No exception thrown for illegal init string.")
        } catch (IllegalArgumentException e) {
            // ignore, pass.
        }
    }
}