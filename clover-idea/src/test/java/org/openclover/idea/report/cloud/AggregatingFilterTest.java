package org.openclover.idea.report.cloud;

import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.metrics.ClassMetrics;
import junit.framework.TestCase;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

/**
 * AggregatingFilter Tester.
 */
@SuppressWarnings({"MagicNumber"})
public class AggregatingFilterTest extends TestCase {
    public void testComplexityAggregation() {
        final AggregatingFilter filter = new AggregatingFilter(null, true);
        ClassMetrics c1 = getCM(1, 10);
        ClassMetrics c2 = getCM(2, 10);
        ClassMetrics c3 = getCM(3, 12);

        final FullClassInfo mockClassInfo = Mockito.mock(FullClassInfo.class);

        Mockito.when(mockClassInfo.getMetrics()).thenReturn(c1);
        filter.accept(mockClassInfo);
        assertEquals(10, filter.complexity.min);
        assertEquals(10, filter.complexity.max);
        assertEquals(10f, filter.minAvgComplexity);
        assertEquals(10f, filter.maxAvgComplexity);

        Mockito.when(mockClassInfo.getMetrics()).thenReturn(c2);
        filter.accept(mockClassInfo);
        assertEquals(10, filter.complexity.min);
        assertEquals(10, filter.complexity.max);
        assertEquals(5f, filter.minAvgComplexity);
        assertEquals(10f, filter.maxAvgComplexity);

        Mockito.when(mockClassInfo.getMetrics()).thenReturn(c3);
        filter.accept(mockClassInfo);
        assertEquals(10, filter.complexity.min);
        assertEquals(12, filter.complexity.max);
        assertEquals(4f, filter.minAvgComplexity);
        assertEquals(10f, filter.maxAvgComplexity);
    }

    public void testElementsAggregation() {
        final AggregatingFilter filter = new AggregatingFilter(null, true);
        ClassMetrics c1 = getCME(10, 0);
        ClassMetrics c2 = getCME(20, 10);
        ClassMetrics c3 = getCME(30, 30);

        final FullClassInfo mockClassInfo = Mockito.mock(FullClassInfo.class);

        Mockito.when(mockClassInfo.getMetrics()).thenReturn(c1);
        filter.accept(mockClassInfo);
        assertEquals(10, filter.elements.min);
        assertEquals(10, filter.elements.max);
        assertEquals(0, filter.uncovered.min);
        assertEquals(0, filter.uncovered.max);

        Mockito.when(mockClassInfo.getMetrics()).thenReturn(c2);
        filter.accept(mockClassInfo);
        assertEquals(10, filter.elements.min);
        assertEquals(20, filter.elements.max);
        assertEquals(0, filter.uncovered.min);
        assertEquals(10, filter.uncovered.max);

        Mockito.when(mockClassInfo.getMetrics()).thenReturn(c3);
        filter.accept(mockClassInfo);
        assertEquals(10, filter.elements.min);
        assertEquals(30, filter.elements.max);
        assertEquals(0, filter.uncovered.min);
        assertEquals(30, filter.uncovered.max);
    }


    private static final Map<String, String[]> MATCHES = new HashMap<String, String[]>() {
        {
            put(null, new String[]{"Class", "pkg.Class", "pkg.subPkg.Class"});
            put(PackageInfo.DEFAULT_PACKAGE_NAME, new String[]{"Class", "pkg.Class", "pkg.subPkg.Class"});
            put("pkg", new String[]{"pkg.Class", "pkg.subPkg.Class"});
            put("pkg.subPkg", new String[]{"pkg.subPkg.Class"});
        }
    };
    private static final Map<String, String[]> MISMATCHES = new HashMap<String, String[]>() {
        {
            put("other", new String[]{"Class", "pkg.Class", "pkg.subPkg.Class", "pkg.other.Class"});
        }
    };


    public void testPrefixMatching() {
        final FullClassInfo mockClassInfo = Mockito.mock(FullClassInfo.class);
        Mockito.when(mockClassInfo.getMetrics()).thenReturn(getCM(1, 1));

        for (Map.Entry<String, String[]> entry : MATCHES.entrySet()) {
            final AggregatingFilter filter = new AggregatingFilter(entry.getKey(), true);
            for (String s : entry.getValue()) {
                Mockito.when(mockClassInfo.getQualifiedName()).thenReturn(s);

                assertTrue(entry.getKey() + "->" + s, filter.accept(mockClassInfo));
            }
        }
        for (Map.Entry<String, String[]> entry : MISMATCHES.entrySet()) {
            final AggregatingFilter filter = new AggregatingFilter(entry.getKey(), true);
            for (String s : entry.getValue()) {
                Mockito.when(mockClassInfo.getQualifiedName()).thenReturn(s);

                assertFalse(entry.getKey() + "->" + s, filter.accept(mockClassInfo));
            }
        }

    }

    private static final Map<String, String[]> MATCHES_NI = new HashMap<String, String[]>() {
        {
            put(null, new String[]{"Class"});
            put(PackageInfo.DEFAULT_PACKAGE_NAME, new String[]{"Class"});
            put("pkg", new String[]{"pkg.Class"});
            put("pkg.subPkg", new String[]{"pkg.subPkg.Class"});
        }
    };
    private static final Map<String, String[]> MISMATCHES_NI = new HashMap<String, String[]>() {
        {
            put("other", new String[]{"Class", "pkg.Class", "pkg.subPkg.Class", "pkg.other.Class"});
            put(null, new String[]{"pkg.Class", "pkg.subPkg.Class"});
            put(PackageInfo.DEFAULT_PACKAGE_NAME, new String[]{"pkg.Class", "pkg.subPkg.Class"});
            put("pkg", new String[]{"Class", "pkg.subPkg.Class"});
            put("pkg.subPkg", new String[]{"Class", "pkg.Class"});
        }
    };

    public void testPrefixMatchingNonInclusive() {
        final FullClassInfo mockClassInfo = Mockito.mock(FullClassInfo.class);
        Mockito.when(mockClassInfo.getMetrics()).thenReturn(getCM(1, 1));

        for (Map.Entry<String, String[]> entry : MATCHES_NI.entrySet()) {
            final AggregatingFilter filter = new AggregatingFilter(entry.getKey(), false);
            for (String s : entry.getValue()) {
                Mockito.when(mockClassInfo.getQualifiedName()).thenReturn(s);

                assertTrue(entry.getKey() + "->" + s, filter.accept(mockClassInfo));
            }
        }
        for (Map.Entry<String, String[]> entry : MISMATCHES_NI.entrySet()) {
            final AggregatingFilter filter = new AggregatingFilter(entry.getKey(), false);
            for (String s : entry.getValue()) {
                Mockito.when(mockClassInfo.getQualifiedName()).thenReturn(s);

                assertFalse(entry.getKey() + "->" + s, filter.accept(mockClassInfo));
            }
        }

    }

    private static ClassMetrics getCM(int numMethods, int complexity) {
        ClassMetrics cm = new ClassMetrics(null);
        cm.setComplexity(complexity);
        cm.setNumMethods(numMethods);
        return cm;
    }

    private static ClassMetrics getCME(int numElements, int uncovered) {
        ClassMetrics cm = new ClassMetrics(null);
        // a bit lying
        cm.setNumStatements(numElements);
        cm.setNumCoveredStatements(numElements - uncovered);
        return cm;
    }

}