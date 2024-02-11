package org.openclover.core.registry

import org.openclover.core.api.instrumentation.ConcurrentInstrumentationException
import org.openclover.core.api.instrumentation.InstrumentationSession
import org.openclover.runtime.api.CloverException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

import static org.junit.Assert.*

class InstrumentationSessionTest {

    @Rule
    public TestName testName = new TestName()

    @Test
    void testEmptySession() throws Exception {
        final Clover2Registry registry = newRegistry(TestUtils.createEmptyDirFor(getClass(), testName.methodName))

        final long startVersion = registry.getVersion()
        registry.startInstr().close()
        final long endVersion = registry.getVersion()

        assertTrue(startVersion != endVersion)
        assertTrue(startVersion < endVersion)
        assertEquals(registry.getProject().getAllPackages().size(), 0)
    }

    @Test
    void testDoubleInstrumentation() throws Exception {
        final Clover2Registry registry = newRegistry(TestUtils.createEmptyDirFor(getClass(), testName.methodName))

        final InstrumentationSession session = registry.startInstr()
        registry.startInstr().close()
        try {
            session.close()
            fail("Should have thrown ConcurrentInstrumentationException")
        } catch (ConcurrentInstrumentationException e) {
            //pass
        }
    }

    private Clover2Registry newRegistry(File parentDir) throws IOException, CloverException {
        File registryFile = File.createTempFile("registry", ".db", parentDir)
        registryFile.deleteOnExit()
        return new Clover2Registry(registryFile, testName.methodName)
    }

}
