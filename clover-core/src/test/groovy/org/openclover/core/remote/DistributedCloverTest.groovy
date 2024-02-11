package org.openclover.core.remote

import org.openclover.runtime.CloverProperties
import org.openclover.runtime.CloverNames
import org.openclover.runtime.Logger
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openclover.runtime.remote.DistributedClover
import org.openclover.runtime.remote.DistributedConfig
import org.openclover.runtime.remote.RpcMessage

import static org.junit.Assert.*

/**
 * Test for {@link DistributedClover}
 */
class DistributedCloverTest {

    final Logger origLogger = Logger.getInstance()
    final RecordingLogger logger = new RecordingLogger()
    DistributedClover dClover

    @Before
    void setUp() {
        Logger.setInstance(logger)
        logger.reset()
    }

    @After
    void tearDown() throws Exception {
        dClover.stop()
        Logger.setInstance(origLogger)
        System.getProperties().remove(CloverNames.PROP_DISTRIBUTED_CONFIG)
        System.getProperties().remove(CloverNames.PROP_SERVER)
    }

    @Test
    void testInitClientServerWithEmptyInitString() {
        dClover = new DistributedClover(new CloverProperties(CloverNames.PROP_DISTRIBUTED_CONFIG, null), null)
        assertFalse(dClover.isServiceMode())
    }

    @Test
    void testEnableWithNonDefaultValues() {
        dClover = new DistributedClover(new CloverProperties(CloverNames.PROP_DISTRIBUTED_CONFIG, "host=127.0.0.1"), null)
        DistributedConfig conf = new DistributedConfig()
        conf.setHost("127.0.0.1")
        assertFalse(dClover.isServiceMode())
        assertTrue(logger.getBuffer().toString(), logger.contains("Distributed coverage is enabled with: " + conf))
    }

    @Test
    void testEnableViaConfigON() {
        dClover = new DistributedClover(new CloverProperties(CloverNames.PROP_DISTRIBUTED_CONFIG, "ON"), null)
        assertFalse(dClover.isServiceMode())
        assertTrue(logger.getBuffer().toString(),
                logger.contains("Distributed coverage is enabled with: " + new DistributedConfig()))
    }

    @Test
    void testEnableViaSysProp() {
        System.setProperty(CloverNames.PROP_DISTRIBUTED_CONFIG, DistributedConfig.ON)
        dClover = new DistributedClover(new CloverProperties(CloverNames.PROP_DISTRIBUTED_CONFIG, ""), null)
        assertFalse(dClover.isServiceMode())
        assertTrue(logger.contains("Distributed coverage is enabled with: " + new DistributedConfig()))
    }

    @Test
    void testDisableViaSysProp() {
        System.setProperty(CloverNames.PROP_DISTRIBUTED_CONFIG, DistributedConfig.OFF)
        dClover = new DistributedClover(new CloverProperties(CloverNames.PROP_DISTRIBUTED_CONFIG, "testing"), null)
        assertFalse(dClover.isServiceMode())
        assertTrue(logger.containsFragment("Distributed coverage is disabled"))
    }

    @Test
    void testInitClientServer() throws InterruptedException {
        final String config = String.format("%s=%s;%s=%s", DistributedConfig.PORT, CajoTcpRecorderServiceTest.TEST_PORT,
                DistributedConfig.NUM_CLIENTS, 1)

        // start a client
        dClover = new DistributedClover(new CloverProperties(CloverNames.PROP_DISTRIBUTED_CONFIG, config), null)
        assertFalse(dClover.isServiceMode())

        // start a server - which will block until the client connects
        System.setProperty(CloverNames.PROP_SERVER, "true")
        DistributedClover serverClover = new DistributedClover(
                new CloverProperties(CloverNames.PROP_DISTRIBUTED_CONFIG, config), null)

        assertTrue(serverClover.isServiceMode())

        // send a message, that will ultimitately fail, due to no registry being available.
        serverClover.remoteFlush(new RpcMessage(RpcMessage.METHOD_START, "testMethod", 2, System.currentTimeMillis()))
    }
}
