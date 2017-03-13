package com.atlassian.clover.ant.types

import org.junit.Test

import static org.junit.Assert.assertEquals

/**
 * Test for {@link com.atlassian.clover.ant.types.BaseCloverOptimizedType}
 */
class BaseCloverOptimizedTypeTest {
    @Test
    void testFileNameNormalisation() {
        assertNormalised("com/atlassian/clover/TestPath.java", "com/atlassian/clover/TestPath.java")
    }

    @Test
    void testMatchingPathForWindowsPaths() {
        assertNormalised("com\\atlassian\\clover\\WindowsTestPath.java", "com/atlassian/clover/WindowsTestPath.java")
    }

    @Test
    void testClassFileNormalization() {
        assertNormalised("com\\atlassian\\clover\\WindowsTestPath.class", "com/atlassian/clover/WindowsTestPath.java")
        assertNormalised("com/atlassian/clover/WindowsTestPath.class", "com/atlassian/clover/WindowsTestPath.java")
    }

    private void assertNormalised(String path, String expectedPath) {
        assertEquals(expectedPath, BaseCloverOptimizedType.normalizePath(path))
    }
}
