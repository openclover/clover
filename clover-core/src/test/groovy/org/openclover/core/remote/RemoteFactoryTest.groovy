package org.openclover.core.remote

import org.junit.Before
import org.junit.Test
import org.openclover.runtime.remote.Config
import org.openclover.runtime.remote.DistributedConfig
import org.openclover.runtime.remote.RecorderListener
import org.openclover.runtime.remote.RecorderService
import org.openclover.runtime.remote.RemoteFactory
import org.openclover.runtime.remote.TcpRecorderListener
import org.openclover.runtime.remote.TcpRecorderService

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class RemoteFactoryTest {

    private final String location =
            DistributedConfig.NAME+"=TCP-SERVER;"+ DistributedConfig.HOST+"=127.0.0.1;"+ DistributedConfig.PORT+"=1111;" + DistributedConfig.TIMEOUT+"=10000"
    private Config config

    @Before
    void setUp() {
        config  = RemoteFactory.getInstance().createConfig(location)
    }

    @Test
    void testCreateConfig() {
        assertTrue(config instanceof DistributedConfig)
        DistributedConfig distributedConfig = (DistributedConfig) config

        assertEquals("TCP-SERVER", distributedConfig.getName())
        assertEquals("127.0.0.1", distributedConfig.getHost())
        assertEquals(1111, distributedConfig.getPort())
        assertEquals(10000, distributedConfig.getTimeout())
    }

    @Test
    void testCreateService() {
        final RecorderService service = RemoteFactory.getInstance().createService(config)
        assertTrue(service instanceof TcpRecorderService)
    }    

    @Test
    void testCreateListener() {
        final RecorderListener listener = RemoteFactory.getInstance().createListener(config)
        assertTrue(listener instanceof TcpRecorderListener)
    }

}
