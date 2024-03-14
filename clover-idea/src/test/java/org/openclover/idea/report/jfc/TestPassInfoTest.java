package org.openclover.idea.report.jfc;

import org.junit.Test;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.context.ContextSetImpl;
import org.openclover.core.registry.FixedSourceRegion;
import org.openclover.core.registry.entities.BasicMethodInfo;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.core.registry.entities.FullTestCaseInfo;
import org.openclover.core.registry.entities.MethodSignature;
import org.openclover.idea.coverage.BaseCoverageNodeViewer;

import java.util.Arrays;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * BaseCoverageNodeViewer Tester.
 */
public class TestPassInfoTest {

    @Test
    public void testTestPassInfo() {
        BaseCoverageNodeViewer.TestPassInfo tpi = new BaseCoverageNodeViewer.TestPassInfo(emptyList());
        verifyTPI(tpi, 0, 0, 0);

        TestCaseInfo[] tcis1 = {
                new FullTestCaseInfo(1, null, fixtureMethod("test1"), "test1-runtime"),
        };
        tpi = new BaseCoverageNodeViewer.TestPassInfo(Arrays.asList(tcis1));
        verifyTPI(tpi, 0, 0, 0);
        assertEquals(-1f, tpi.getPcTestErrors(), 0.0f);
        assertEquals(-1f, tpi.getPcTestFailures(), 0.0f);
        assertEquals(-1f, tpi.getPcTestPasses(), 0.0f);

        TestCaseInfo[] tcis2 = {
                new TestCaseInfoMock(TestCaseInfoMock.Type.NORESULT),
        };
        tpi = new BaseCoverageNodeViewer.TestPassInfo(Arrays.asList(tcis2));
        verifyTPI(tpi, 0, 0, 0);

        TestCaseInfo[] tcis3 = {
                new TestCaseInfoMock(TestCaseInfoMock.Type.PASS),
                new TestCaseInfoMock(TestCaseInfoMock.Type.FAIL),
                new TestCaseInfoMock(TestCaseInfoMock.Type.ERROR),
                new TestCaseInfoMock(TestCaseInfoMock.Type.PASS),
                new TestCaseInfoMock(TestCaseInfoMock.Type.PASS),
                new TestCaseInfoMock(TestCaseInfoMock.Type.FAIL),
                new TestCaseInfoMock(TestCaseInfoMock.Type.NORESULT),
        };
        tpi = new BaseCoverageNodeViewer.TestPassInfo(Arrays.asList(tcis3));
        verifyTPI(tpi, 3, 2, 1);
        assertEquals(0.5f, tpi.getPcTestPasses(), 0.0f);
        assertTrue(0.33f < tpi.getPcTestFailures() && 0.34f > tpi.getPcTestFailures());
        assertTrue(0.16f < tpi.getPcTestErrors() && 0.17f > tpi.getPcTestErrors());
    }

    private static void verifyTPI(BaseCoverageNodeViewer.TestPassInfo tpi, int passed, int failed, int errors) {
        assertEquals(passed, tpi.getTestPasses());
        assertEquals(errors, tpi.getTestErrors());
        assertEquals(failed, tpi.getTestFailures());
        final int runs = passed + errors + failed;
        assertEquals(runs, tpi.getTestsRun());
        assertEquals(runs != 0, tpi.hasResults());
    }

    private static class TestCaseInfoMock extends FullTestCaseInfo {
        private static int nextId;

        enum Type {
            PASS, FAIL, ERROR, NORESULT
        }

        private final Type type;

        public TestCaseInfoMock(Type type) {
            super(++nextId, null, fixtureMethod("test" + nextId), "test" + nextId + "-runtime");
            this.type = type;
        }

        @Override
        public boolean isError() {
            return type == Type.ERROR;
        }

        @Override
        public boolean isFailure() {
            return type == Type.FAIL;
        }

        @Override
        public boolean isSuccess() {
            return type == Type.PASS;
        }

        @Override
        public boolean isHasResult() {
            return type != Type.NORESULT;
        }
    }

    private static FullMethodInfo fixtureMethod(String name) {
        return new FullMethodInfo((FullClassInfo)null, new ContextSetImpl(),
                new BasicMethodInfo(new FixedSourceRegion(0, 0), 0, 0, new MethodSignature(name), true, null, false) );
    }
}
