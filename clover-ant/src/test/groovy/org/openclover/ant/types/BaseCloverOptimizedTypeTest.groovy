package org.openclover.ant.types

import org.junit.Test

import static org.junit.Assert.assertEquals

/**
 * Test for {@link org.openclover.ant.types.BaseCloverOptimizedType}
 */
class BaseCloverOptimizedTypeTest {
    @Test
    void testFileNameNormalisation() {
        assertNormalised("org/openclover/TestPath.java", "org/openclover/TestPath.java")
    }

    @Test
    void testMatchingPathForWindowsPaths() {
        assertNormalised("org\\openclover\\WindowsTestPath.java", "org/openclover/WindowsTestPath.java")
    }

    @Test
    void testClassFileNormalization() {
        assertNormalised("org\\openclover\\WindowsTestPath.class", "org/openclover/WindowsTestPath.java")
        assertNormalised("org/openclover/WindowsTestPath.class", "org/openclover/WindowsTestPath.java")
    }

    private static void assertNormalised(String path, String expectedPath) {
        assertEquals(expectedPath, BaseCloverOptimizedType.normalizePath(path))
    }
}
