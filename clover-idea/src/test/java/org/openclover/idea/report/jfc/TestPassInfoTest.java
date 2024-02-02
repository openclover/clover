package org.openclover.idea.report.jfc;

import com.atlassian.clover.context.ContextSet;
import org.openclover.idea.coverage.BaseCoverageNodeViewer;
import com.atlassian.clover.registry.FixedSourceRegion;
import com.atlassian.clover.registry.entities.BasicMethodInfo;
import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * BaseCoverageNodeViewer Tester.
 */
public class TestPassInfoTest {

    @Test
    public void testTestPassInfo() {
        BaseCoverageNodeViewer.TestPassInfo tpi = new BaseCoverageNodeViewer.TestPassInfo(Collections.<TestCaseInfo>emptyList());
        verifyTPI(tpi, 0, 0, 0);

        TestCaseInfo[] tcis1 = {
                new TestCaseInfo(1, null, fixtureMethod("test1"), "test1-runtime"),
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

    private static class TestCaseInfoMock extends TestCaseInfo {
        private static int nextId;

        static enum Type {
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
        return new FullMethodInfo((FullClassInfo)null, new ContextSet(),
                new BasicMethodInfo(new FixedSourceRegion(0, 0), 0, 0, new MethodSignature(name), true, null, false) );
    }
}
