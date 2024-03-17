package org.openclover.core.reporters.html

import junit.framework.TestCase
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.metrics.BlockMetrics
import org.openclover.core.registry.metrics.HasMetricsTestFixture

import static org.openclover.core.util.Lists.newArrayList

class StatisticsClassInfoVisitorTest extends TestCase {

    List<ClassInfo> classes = newArrayList()
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
        for (Iterator<ClassInfo> iterator = classes.iterator(); iterator.hasNext();) {
            ClassInfo info = iterator.next()
            assertTrue(Arrays.binarySearch(prevHash, info.hashCode()) < 0)
            prevHash[i++] = info.hashCode()
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

        ClassInfo[] classInfos = v1Classes.toArray(new ClassInfo[3])
        //assertEquals(classInfos[1].getName()) //TODO assert order is correct.

    }

    private static class ClassComparator implements Comparator {
        private final StatisticsClassInfoVisitor v1

        ClassComparator(StatisticsClassInfoVisitor v1) {
            this.v1 = v1
        }

        int compare(Object object, Object object1) {
            ClassInfo b1 = (ClassInfo) object
            ClassInfo b2 = (ClassInfo) object1
            return v1.getCalculator().getScaledValue(b1) - v1.getCalculator().getScaledValue(b2)
        }
    }
}
