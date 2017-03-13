package com.atlassian.clover.reporters.html

import clover.com.google.common.collect.Lists
import com.atlassian.clover.registry.entities.BaseClassInfo
import com.atlassian.clover.registry.metrics.BlockMetrics
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.registry.metrics.HasMetricsTestFixture
import junit.framework.TestCase

class StatisticsClassInfoVisitorTest extends TestCase {

    List classes = Lists.newArrayList()
    HasMetricsTestFixture fixture
    FullFileInfo fileInfo 

    StatisticsClassInfoVisitorTest() throws IOException {
        fixture = new HasMetricsTestFixture("test")
        fileInfo = fixture.getDefaultFileInfo()
    }

    void setUp() {
        setQuickWin(addClass(), 1, 5, 5)
        setQuickWin(addClass(), 2, 12, 6)
        setQuickWin(addClass(), 3, 21, 7)
        setQuickWin(addClass(), 3, 15, 0)
        setQuickWin(addClass(), 1, 0, 0)
        assertHashesUnique()
    }

    private BlockMetrics addClass() {
        BlockMetrics classMetrics = (BlockMetrics)fixture.newClass(fileInfo, "TestClass-" + classes.size(), classes.size() + 1).getMetrics()
        classes.add(classMetrics.getOwner())
        return classMetrics
    }

    private void assertHashesUnique() {
        int[] prevHash = new int[classes.size()]
        int i = 0
        for (Iterator iterator = classes.iterator(); iterator.hasNext();) {
            BaseClassInfo baseClassInfo = (BaseClassInfo) iterator.next()
            assertTrue(Arrays.binarySearch(prevHash, baseClassInfo.hashCode()) < 0)
            prevHash[i++] = baseClassInfo.hashCode()
            Arrays.sort(prevHash)
        }
    }

    private void setQuickWin(BlockMetrics bm, int cmp, int numEle, int uncovered) {
        bm.setComplexity(cmp)
        bm.setNumStatements(numEle)
        bm.setNumCoveredStatements(numEle - uncovered)

    }


    void testQuickWins() {
        final ClassInfoStatsCalculator totalEle = new ClassInfoStatsCalculator.ElementCountCalculator()
        final ClassInfoStatsCalculator uncoveredEle = new ClassInfoStatsCalculator.CoveredElementsCalculator()

        final StatisticsClassInfoVisitor totalEleStats = StatisticsClassInfoVisitor.visit(classes, totalEle)
        final StatisticsClassInfoVisitor coveredStats = StatisticsClassInfoVisitor.visit(totalEleStats.getClasses(), uncoveredEle)

        List visitedClasses = coveredStats.getClasses()
        assertEquals(3, visitedClasses.size())
        assertEquals(5, totalEleStats.getMin())
        assertEquals(21, totalEleStats.getMax())
        assertEquals(16, totalEleStats.getRange())

        assertEquals(3, coveredStats.getClasses().size())
        assertEquals(6, coveredStats.getMin())
        assertEquals(15, coveredStats.getMax())
        assertEquals(9, coveredStats.getRange())

        // check that values are ordered correctly.
        List v1Classes = totalEleStats.getClasses()
        Collections.sort(v1Classes, new ClassComparator(totalEleStats))

        BaseClassInfo[] classInfos = (BaseClassInfo[]) v1Classes.toArray(new BaseClassInfo[3])
        //assertEquals(classInfos[1].getName()) //TODO assert order is correct.

    }

    private static class ClassComparator implements Comparator {
        private final StatisticsClassInfoVisitor v1

        ClassComparator(StatisticsClassInfoVisitor v1) {
            this.v1 = v1
        }

        int compare(Object object, Object object1) {
            BaseClassInfo b1 = (BaseClassInfo) object
            BaseClassInfo b2 = (BaseClassInfo) object1
            return v1.getCalculator().getScaledValue(b1) - v1.getCalculator().getScaledValue(b2)
        }
    }
}
