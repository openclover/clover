package org.openclover.core.remote

import org.junit.After
import org.junit.Test
import org.openclover.core.util.RecordingLogger
import org.openclover.runtime.Logger
import org.openclover.runtime.remote.DistributedConfig
import org.openclover.runtime.remote.RemoteFactory
import org.openclover.runtime.remote.TcpRecorderListener
import org.openclover.runtime.remote.TcpRecorderService

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class TcpRecorderListenerTest {

    private final Logger origLogger = Logger.getInstance()
    private TcpRecorderListener listener
    private TcpRecorderService service

    @After
    void tearDown() {
        listener?.disconnect()
        service?.stop()
        Logger.setInstance(origLogger)
    }

    private DistributedConfig config() {
        (DistributedConfig) RemoteFactory.getInstance().createConfig(
                "${DistributedConfig.PORT}=${TcpRecorderServiceTest.TEST_PORT}")
    }

    @Test
    void testConnectFailsGracefullyWithoutServer() {
        final DistributedConfig config = config()
        listener = (TcpRecorderListener) RemoteFactory.getInstance().createListener(config)
        final RecordingLogger logger = new RecordingLogger()
        Logger.setInstance(logger)

        listener.connect()
        Thread.sleep(1000)

        // no server is listening, so the reconnect loop keeps failing softly - never throwing
        assertTrue(logger.getBufferAsString(), logger.containsFragment("Could not connect to server"))
    }

    @Test
    void testListenerRegistersWithService() {
        final DistributedConfig config = config()
        service = (TcpRecorderService) RemoteFactory.getInstance().createService(config)
        service.start()

        listener = (TcpRecorderListener) RemoteFactory.getInstance().createListener(config)
        listener.connect()

        final long deadline = System.currentTimeMillis() + 5000
        while (service.getNumRegisteredListeners() < 1 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
        assertEquals(1, service.getNumRegisteredListeners())
    }
}
