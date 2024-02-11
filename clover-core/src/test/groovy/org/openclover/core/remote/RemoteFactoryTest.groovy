package org.openclover.core.remote

import org.junit.Before
import org.junit.Test
import org.openclover.runtime.remote.CajoTcpRecorderListener
import org.openclover.runtime.remote.CajoTcpRecorderService
import org.openclover.runtime.remote.Config
import org.openclover.runtime.remote.DistributedConfig
import org.openclover.runtime.remote.RecorderListener
import org.openclover.runtime.remote.RecorderService
import org.openclover.runtime.remote.RemoteFactory

import static org.junit.Assert.*

class RemoteFactoryTest {

    private final String location =
            DistributedConfig.NAME+"=CAJO-TCP;"+ DistributedConfig.HOST+"=127.0.0.1;"+ DistributedConfig.PORT+"=1111;" + DistributedConfig.TIMEOUT+"=10000"
    private Config config

    @Before
    void setUp() {
        config  = RemoteFactory.getInstance().createConfig(location)
    }

    @Test
    void testCreateConfig() {
        assertTrue(config instanceof DistributedConfig)
        DistributedConfig distributedConfig = (DistributedConfig) config

        assertEquals("CAJO-TCP", distributedConfig.getName())
        assertEquals("127.0.0.1", distributedConfig.getHost())
        assertEquals(1111, distributedConfig.getPort())
        assertEquals(10000, distributedConfig.getTimeout())
    }

    @Test
    void testCreateService() {
        final RecorderService service = RemoteFactory.getInstance().createService(config)
        assertTrue(service instanceof CajoTcpRecorderService)
    }    

    @Test
    void testCreateListener() {
        final RecorderListener listener = RemoteFactory.getInstance().createListener(config)
        assertTrue(listener instanceof CajoTcpRecorderListener)
    }

}
