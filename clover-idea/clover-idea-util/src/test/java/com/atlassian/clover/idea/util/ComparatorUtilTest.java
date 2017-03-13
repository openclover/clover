package com.atlassian.clover.idea.util;

import junit.framework.TestCase;

/**
 * ComparatorUtil Tester.
 */
public class ComparatorUtilTest extends TestCase {
    public void testComparator() {
        assertEquals(0, ComparatorUtil.compare(null, null));
        assertEquals(-1, ComparatorUtil.compare(null, "A"));
        assertEquals(1, ComparatorUtil.compare("A", null));
        assertEquals(0, ComparatorUtil.compare("A", "A"));
        assertEquals(-1, ComparatorUtil.compare("A", "B"));
        assertEquals(1, ComparatorUtil.compare("B", "A"));

        assertEquals(0, ComparatorUtil.compareNE(null, null));
        assertEquals(0, ComparatorUtil.compareNE(null, "A"));
        assertEquals(0, ComparatorUtil.compareNE("A", null));
        assertEquals(0, ComparatorUtil.compareNE("A", "A"));
        assertEquals(-1, ComparatorUtil.compareNE("A", "B"));
        assertEquals(1, ComparatorUtil.compareNE("B", "A"));

        assertEquals(0, ComparatorUtil.compareLong(0l, 0l));
        assertEquals(-1, ComparatorUtil.compareLong(0l, 1l));
        assertEquals(1, ComparatorUtil.compareLong(1l, 0l));

    }
}
