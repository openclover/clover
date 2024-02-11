package org.openclover.core.instr.java

import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig
import com.atlassian.clover.instr.java.InstrumentationSource
import com.atlassian.clover.instr.java.Instrumenter
import com.atlassian.clover.instr.java.StringInstrumentationSource
import com.atlassian.clover.util.FileUtils
import com.atlassian.clover.registry.entities.FullProjectInfo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

public class ExpressionInfoTest {

    @Rule
    public TestName testName = new TestName()

    private File workingDir

    @Before
    void setUp() throws Exception {
        workingDir = File.createTempFile(getClass().getName() + "." + testName.methodName, ".tmp")
        workingDir.delete()
        workingDir.mkdir()
    }

    @After
    void tearDown() throws Exception {
        FileUtils.deltree(workingDir)
    }

    @Test
    void testConstantExprDetection() throws Exception {
        assertTrue(isNonConstant("foo"))
        assertTrue(isNonConstant("foo == 3"))
        assertTrue(isNonConstant("foo != 3"))
        assertTrue(isNonConstant("foo == bar"))
        assertFalse(isNonConstant("1 == 1"))
        assertFalse(isNonConstant("(1 + 4) * 6 < 3.5f"))
        assertFalse(isNonConstant("true == (1*6 > 8 + 1)"))
        assertFalse(isNonConstant("1 != 2"))
        assertFalse(isNonConstant("1 >> 2 == 3"))
        assertFalse(isNonConstant("1 << 2 != 3"))
        assertFalse(isNonConstant("1 & 2 == 3"))
        assertFalse(isNonConstant("1 ^ 2 == 3"))
    }

    private boolean isNonConstant(final String expression) throws Exception {
        return containsNonConstantExpr("class a{a(){if ("+expression+") {};}}")
    }

    private boolean containsNonConstantExpr(final String sourceCode) throws Exception {
        // create input source
        final InstrumentationSource input = new StringInstrumentationSource(workingDir, sourceCode)

        // perform instrumentation and get the model
        final Instrumenter instr = new Instrumenter(getCfg())
        instr.startInstrumentation()
        instr.instrument(input, new StringWriter(), null);                       // ignore the output,
        final FullProjectInfo model = instr.endInstrumentation().getProject(); // just take the model

        return model.getMetrics().getNumBranches() != 0
    }

    private JavaInstrumentationConfig getCfg() throws IOException {
        JavaInstrumentationConfig cfg = new JavaInstrumentationConfig()
        File db = File.createTempFile("clv",".db", workingDir)
        db.delete()

        cfg.setInitstring(db.getAbsolutePath())
        return cfg
    }

}
