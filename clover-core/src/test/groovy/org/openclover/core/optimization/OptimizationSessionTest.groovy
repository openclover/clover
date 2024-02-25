package org.openclover.core.optimization

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openclover.core.api.optimization.OptimizationOptions
import org.openclover.core.util.RecordingLogger
import org.openclover.runtime.Logger

import static org.junit.Assert.assertTrue

class OptimizationSessionTest {
    private RecordingLogger bufferLogger
    private Logger oldLogger

    @Before
    void setUp() throws Exception {
        oldLogger = Logger.getInstance()
        bufferLogger = new RecordingLogger()
        Logger.setInstance(bufferLogger)
    }

    @After
    void tearDown() throws Exception {
        Logger.setInstance(oldLogger)
    }

    @Test
    void testOptimizableNameSinglularLogging() {
        assertOptimizationLog("icle", 1, 1, 1, 1000, " test icle ")
    }

    @Test
    void testOptimizableNamePluralLogging() {
        assertOptimizationLog("icle", 2, 2, 2, 1000, " test icles ")
    }

    @Test
    void testSomeSavings() {
        assertOptimizationLog("class", 1, 1, 1, 2000, "Clover estimates having saved around 2 seconds on this optimized test run")
    }

    @Test
    void testNoSavings() {
        assertOptimizationLog("class", 1, 1, 1, 0, "Clover was unable to save any time on this optimized test run")
    }

    @Test
    void testNoAutoSummarize() {
        OptimizationSession session =
            new OptimizationSession(
                new OptimizationOptions.Builder().optimizableName("icle").build(), true)


        session.incFoundOptimizableCount(1)
        session.incOriginalOptimizableCount(1)
        session.incOptimizedOptimizableCount(1)
        session.incSavings(1000)

        session.afterOptimizaion(true)

        assertLogged(" test icle ")
    }

    private void assertOptimizationLog(
        String optimizableName, int foundCount, int originalCount, int optimizedCount, long savings, String... fragments) {
        OptimizationSession session =
            new OptimizationSession(
                new OptimizationOptions.Builder().optimizableName(optimizableName).build(), true)


        session.incFoundOptimizableCount(foundCount)
        session.incOriginalOptimizableCount(originalCount)
        session.incOptimizedOptimizableCount(optimizedCount)
        session.incSavings(savings)

        session.summarize()

        assertLogged(fragments)
    }

    private void assertLogged(String... fragments) {
        for(String fragment : fragments) {
            assertTrue("Log did not contain line \"" + fragment + "\":\n" + bufferLogger.getBufferAsString(), bufferLogger.containsFragment(fragment))
        }
    }
}
