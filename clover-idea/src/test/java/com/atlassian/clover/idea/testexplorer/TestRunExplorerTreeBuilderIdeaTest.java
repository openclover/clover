package com.atlassian.clover.idea.testexplorer;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.CoverageDataSpec;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.idea.coverage.CoverageManager;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.reporters.filters.DefaultTestFilter;
import com.intellij.testFramework.LightIdeaTestCase;
import org.mockito.Mockito;

import java.util.Collection;

@SuppressWarnings({"MagicNumber"})
public class TestRunExplorerTreeBuilderIdeaTest extends LightIdeaTestCase {
    private CloverDatabase cloverDb;
    private final String path = getClass().getResource("/clover/coverage.db").getPath();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cloverDb = new CloverDatabase(path);
        cloverDb.loadCoverageData(new CoverageDataSpec(new DefaultTestFilter(), 0));
    }

    public void testUniqueCoverage() {
        FullProjectInfo projectInfo = cloverDb.getFullModel();

        FullClassInfo aClass = (FullClassInfo) projectInfo.findClass("com.cenqua.clovertest.A");
        assertNotNull(aClass);


        final CoverageManager coverageManager = Mockito.mock(CoverageManager.class);
        Mockito.when(coverageManager.getCoverage()).thenReturn(cloverDb);
        
        @SuppressWarnings("unchecked")
        Collection<DecoratedTestCaseInfo> testCases = TestRunExplorerTreeBuilder.wrap(cloverDb.getCoverageData().getTests(), aClass, cloverDb, coverageManager);
        int count = 0;
        for (DecoratedTestCaseInfo testCase : testCases) {
            if ("testMethod".equals(testCase.getTestName())) {
                assertEquals(0.3125, testCase.getCoverage(), 0.0001);
                assertEquals(0.0625, testCase.getUniqueCoverage(), 0.0001);
                ++count;
            }
            if ("testMethod1".equals(testCase.getTestName())) {
                assertEquals(0.375, testCase.getCoverage(), 0.0001);
                assertEquals(0.125, testCase.getUniqueCoverage(), 0.0001);
                ++count;
            }
        }

        assertEquals("Sanity check - have both test cases been found?", 2, count);

    }

    public void testObsoleteCalculation() throws CloverException {
        FullClassInfo aClass = (FullClassInfo) cloverDb.getFullModel().findClass("com.cenqua.clovertest.A");

        final CoverageManager coverageManager = Mockito.mock(CoverageManager.class);
        Mockito.when(coverageManager.getCoverage()).thenReturn(new CloverDatabase(path));

        @SuppressWarnings("unchecked")
        Collection<DecoratedTestCaseInfo> testCases = TestRunExplorerTreeBuilder.wrap(cloverDb.getCoverageData().getTests(), aClass, cloverDb, coverageManager);
        assertFalse(testCases.isEmpty());
        for (DecoratedTestCaseInfo testCase : testCases) {
            assertEquals(-1f, testCase.getCoverage(), 0.0001);
            assertEquals(-1f, testCase.getUniqueCoverage(), 0.0001);
        }
        
    }
}
