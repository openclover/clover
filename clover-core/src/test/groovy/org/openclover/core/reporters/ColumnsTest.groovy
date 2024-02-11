package org.openclover.core.reporters

import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ColumnsTest {
    @Test
    void testValidColumnName() {
        assertTrue(Columns.isValidColumnName("totalElements"))
        assertFalse(Columns.isValidColumnName("fooBar"))
    }
}
