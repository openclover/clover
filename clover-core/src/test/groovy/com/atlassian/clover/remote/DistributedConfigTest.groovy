package com.atlassian.clover.remote

import org.junit.Test
import org.openclover.runtime.remote.DistributedConfig

import static org.junit.Assert.*

class DistributedConfigTest {

    @Test
    void testSetters() {
        final DistributedConfig config = new DistributedConfig()
        config.setName("my.name")
        config.setHost("myhost")
        config.setPort(1111)
        config.setNumClients(2)
        config.setTimeout(1000)
        config.setRetryPeriod(500)

        assertTrue(config.isEnabled())
        assertEquals("my.name", config.getName())
        assertEquals("myhost", config.getHost())
        assertEquals(1111, config.getPort())
        assertEquals(2, config.getNumClients())
        assertEquals(1000, config.getTimeout())
        assertEquals(500, config.getRetryPeriod())
    }

    @Test
    void testDefaults() {
        final DistributedConfig config = new DistributedConfig()
        assertDefaults(config)
    }

    private void assertDefaults(DistributedConfig config) {
        assertTrue(config.isEnabled())
        assertEquals("localhost", config.getHost())
        assertEquals(1198, config.getPort())
        assertEquals(0, config.getNumClients())
        assertEquals(5000, config.getTimeout())
        assertEquals(1000, config.getRetryPeriod())
    }

    @Test
    void testOn() {
        final DistributedConfig config = DistributedConfig.ON()
        assertDefaults(config)
    }

    @Test
    void testOff() {
        final DistributedConfig config = DistributedConfig.OFF()
        assertFalse(config.isEnabled())
        try {
            config.getHost()
            fail("No NPE thrown when getHost called on a disabled config object")
        } catch (NullPointerException e) {
            // ignore, expected.
        }
    }

    @Test
    void testToStringWhenOff() {
        final DistributedConfig config = DistributedConfig.OFF()
        assertNull(config.toString())
    }

    @Test
    void testToStringWhenOn() {
        final DistributedConfig config = DistributedConfig.ON()
        assertNotNull(config.toString())
    }
}
