package com.atlassian.clover.remote

import com.atlassian.clover.util.RecordingLogger
import org.openclover.runtime.Logger
import org.junit.Test
import org.openclover.runtime.remote.CajoTcpRecorderListener
import org.openclover.runtime.remote.DistributedConfig

class CajoTcpRecorderListenerTest {

    @Test
    void testConnect() throws InterruptedException {
        DistributedConfig config = new DistributedConfig(DistributedConfig.PORT + "=" + CajoTcpRecorderServiceTest.TEST_PORT)
        CajoTcpRecorderListener listener =
                (CajoTcpRecorderListener) RemoteFactory.getInstance().createListener(config)
        RecordingLogger logger = new RecordingLogger()
        Logger origLogger = Logger.getInstance()
        try {
            Logger.setInstance(logger)
            listener.connect()
            Thread.sleep(1000)
            logger.contains("Attempting connection to: ${listener.getConnectionUrl()}")
            logger.containsFragment("Could not connect to server at ${listener.getConnectionUrl()}")
        } finally {
            listener.disconnect()
            Logger.setInstance(origLogger)
        }
    }
}