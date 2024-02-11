package org.openclover.core.util

import org.junit.Test
import org.openclover.util.ClassPathUtil

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/**
 * Test for {@link org.openclover.util.ClassPathUtil}
 */
class ClassPathUtilTest {

    @Test
    void testGetCloverJarPath() throws Exception {
        final String path = ClassPathUtil.getCloverJarPath()
        assertNotNull(path)
        assertTrue(new File(path).exists())
    }
}