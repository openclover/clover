package org.openclover.ci

import org.junit.Before
import org.junit.Test
import org.openclover.runtime.CloverNames

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.openclover.core.util.Lists.newArrayList

/**
 * Test for {@link org.openclover.ci.AntIntegrator}
 */
class AntIntegratorTest {
    List<String> args

    @Before
    void setUp() {
        args = newArrayList("clean", "test")
    }

    @Test
    void testDecorateArguments() {
        CIOptions.Builder options = new CIOptions.Builder()
        Integrator ant = Integrator.Factory.newAntIntegrator(options.fullClean(true).build())

        ant.decorateArguments(args)
        assertEquals("clover.fullclean", args.get(0))
        assertTrue(args.contains(AntIntegrationListener.class.getName()))
        assertTrue(!args.contains("-Dclover.skip.current=true"))
        assertTrue(args.contains("-Dclover.skip.report=true"))
    }

    @Test
    void testDecorateArgumentsHistorical() {
        CIOptions.Builder options = new CIOptions.Builder()

        final File historyDir = new File(".historydir")
        options.historical(true).historyDir(historyDir)
        Integrator ant = Integrator.Factory.newAntIntegrator(options.build())
        ant.decorateArguments(args)
        assertTrue(!args.contains("-Dclover.skip.report=true"))
        assertTrue(args.contains("-Dclover.historydir=${historyDir.getAbsolutePath()}".toString()))
    }

    @Test
    void testDecorateArgumentsJSON() {
        CIOptions.Builder options = new CIOptions.Builder()
        options.json(true).historical(true)
        Integrator ant = Integrator.Factory.newAntIntegrator(options.build())
        ant.decorateArguments(args)
        assertTrue(!args.contains("-Dclover.skip.json=true"))
        assertTrue(!args.contains("-Dclover.skip.report=true"))
    }

    @Test
    void testDecorateArgumentsOptimize() {
        CIOptions.Builder options = new CIOptions.Builder()

        options.optimize(true)
        Integrator ant = Integrator.Factory.newAntIntegrator(options.build())

        ant.decorateArguments(args)
        assertTrue(args.contains("-D${CloverNames.PROP_CLOVER_OPTIMIZATION_ENABLED}=${options.build().isOptimize()}".toString()))
    }

    private static void assertArgument(CIOptions options, String osName, String expectedArgument) {
        List<String> args = []
        String originalName = System.getProperty("os.name")
        try {
            System.setProperty("os.name", osName)
            Integrator.Factory.newAntIntegrator(options).decorateArguments(args)
            assertTrue("Expected '${expectedArgument}' but found ${args}", args.contains(expectedArgument))
        } finally {
            System.setProperty("os.name", originalName)
        }
    }
}