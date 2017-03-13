package com.atlassian.clover.util

import org.junit.Test

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/**
 * Test for {@link com.atlassian.clover.util.ClassPathUtil}
 */
class ClassPathUtilTest {

    @Test
    void testGetCloverJarPath() throws Exception {
        final String path = ClassPathUtil.getCloverJarPath()
        assertNotNull(path)
        assertTrue(new File(path).exists())
    }
}