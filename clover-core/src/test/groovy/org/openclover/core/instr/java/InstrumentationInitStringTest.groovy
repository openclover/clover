package org.openclover.core.instr.java

import org.junit.Test
import org.openclover.core.cfg.instr.InstrumentationConfig

import static org.junit.Assert.assertTrue

class InstrumentationInitStringTest extends InstrumentationTestBase {
    @Test
    void testDefaultInitString() throws Exception {
        final File dbDir = new File(workingDir, InstrumentationConfig.DEFAULT_DB_DIR)
        final File dbFile = new File(dbDir, InstrumentationConfig.DEFAULT_DB_FILE)


        String defaultInitStr = dbFile.getAbsolutePath()

        String out = getInstrumentedVersion(null, false, "class B { B(int arg) {}}")

        String expectedCharArray = RecorderInstrEmitter.asUnicodeString(defaultInitStr)

        assertTrue(out.contains(expectedCharArray))
    }

    @Test
    void testRelativeDefaultInitString() throws Exception {
        String defaultRelInitStr = InstrumentationConfig.DEFAULT_DB_DIR + File.separator + InstrumentationConfig.DEFAULT_DB_FILE


        String out = getInstrumentedVersion(null, true, "class B { B(int arg) {}}")
        String expectedCharArray = RecorderInstrEmitter.asUnicodeString(defaultRelInitStr)

        assertTrue(out.contains(expectedCharArray))
        File file = new File(defaultRelInitStr)
        assertTrue("Could not delete file referenced by default initstring: " + file.getAbsolutePath(), file.delete())
    }

    @Test
    void testRelativeInitString() throws Exception {
        File coverageDbFile = File.createTempFile(getClass().getName() +"." + name, ".tmp", workingDir)
        coverageDbFile.delete()

        String out = getInstrumentedVersion(coverageDbFile.getPath(), true, "class B { B(int arg) {}}")

        String expectedCharArray = RecorderInstrEmitter.asUnicodeString(coverageDbFile.getPath())

        assertTrue(out.contains(expectedCharArray))
    }

    @Test
    void testInitString() throws Exception {
        File coverageDbFile = File.createTempFile(getClass().getName() +"." + name, ".tmp", workingDir)
        coverageDbFile.delete()

        String out = getInstrumentedVersion(coverageDbFile.getAbsolutePath(), false, "class B { B(int arg) {}}")

        String expectedCharArray = RecorderInstrEmitter.asUnicodeString(coverageDbFile.getAbsolutePath())

        assertTrue(out.contains(expectedCharArray))
    }
}
