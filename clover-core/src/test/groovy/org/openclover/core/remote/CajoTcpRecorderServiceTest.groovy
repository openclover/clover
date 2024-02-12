package org.openclover.core.remote

import org.junit.Ignore
import org.junit.Test
import org.openclover.core.util.RecordingLogger
import org.openclover.runtime.Logger
import org.openclover.runtime.remote.CajoTcpRecorderListener
import org.openclover.runtime.remote.CajoTcpRecorderService
import org.openclover.runtime.remote.DistributedConfig
import org.openclover.runtime.remote.RpcMessage

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Test for {@link org.openclover.runtime.remote.CajoTcpRecorderService}
 */
@Ignore
class CajoTcpRecorderServiceTest {

    private static final String KEY_TCP_PORT = "cajo.tcp.port"

    final String type = "test.Type"
    final Long startTime = System.currentTimeMillis()
    final Integer sliceId = 1
    final RpcMessage msg = new RpcMessage(RpcMessage.METHOD_START, type, sliceId, startTime)
    private static final String DEFAULT_PORT = "1199"
    static final String TEST_PORT = System.getProperty(KEY_TCP_PORT, DEFAULT_PORT)

    // to be visible to cajo, this class can not be anonymous-inner
    class StubbedCajoTcpRecorderListener extends CajoTcpRecorderListener {
        @Override
        void allRecordersSliceStart(String t, Integer s, Long time) {
            assertEquals(type, t)
            assertEquals(sliceId, s)
            assertEquals(startTime, time)
        }
    }
    
    /**
     * Start a RecorderService, have a listener register to it, send a message and then stop the service.
     * @throws InterruptedException
     */
    @Test
    void testStartServiceAndConnectListener() throws InterruptedException {
        final int timeout = 2000; // ms
        final DistributedConfig config = (DistributedConfig) RemoteFactory.getInstance().createConfig(
                "${DistributedConfig.PORT}=${TEST_PORT};${DistributedConfig.TIMEOUT}=${timeout}")
        
        final Logger origLogger = Logger.getInstance()
        final CajoTcpRecorderListener stubbedListener = new StubbedCajoTcpRecorderListener()
        final CajoTcpRecorderService service =
                (CajoTcpRecorderService) RemoteFactory.getInstance().createService(config)
        stubbedListener.init(config)

        try {
            final RecordingLogger logger = new RecordingLogger()
            Logger.setInstance(logger)
            service.start()
            assertTrue(logger.contains("About to start service with config: ${config}"))
            assertEquals(0, service.getNumRegisteredListeners())
            Logger.setInstance(origLogger)

            stubbedListener.connect()
            Thread.sleep((timeout * 3)/2 as int); // time for connection and registration
            assertEquals(1, service.getNumRegisteredListeners())

            final Integer numMessagesSent = (Integer) service.sendMessage(msg)
            assertEquals(1, numMessagesSent.intValue())
            // assert that message was sent is done in the StubbedListener
        } finally {
            stubbedListener.disconnect()
            Thread.sleep(config.getRetryPeriod())
            service.stop()
            Logger.setInstance(origLogger)
        }
    }

    class TimeoutCajoTcpRecorderListener extends CajoTcpRecorderListener {
        @Override
        void allRecordersSliceStart(String t, Integer s, Long time) {
            try {
                Thread.sleep(1000)
            } catch (InterruptedException e) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Start a RecorderService, have a listener register to it, send a message and then stop the service.
     * @throws InterruptedException
     */
    @Test
    void testTimeout() throws InterruptedException {
        final CajoTcpRecorderListener timeoutListener = new TimeoutCajoTcpRecorderListener()
        final DistributedConfig config = (DistributedConfig) RemoteFactory.getInstance().createConfig(
                "${DistributedConfig.PORT}=${TEST_PORT};${DistributedConfig.TIMEOUT}=500;${DistributedConfig.NUM_CLIENTS}=1")
        timeoutListener.init(config)

        final CajoTcpRecorderService service = (CajoTcpRecorderService) RemoteFactory.getInstance().createService(config)

        final Logger origLogger = Logger.getInstance();        
        final RecordingLogger logger = new RecordingLogger()
        Logger.setInstance(logger)
        
        try {
            timeoutListener.connect(); // start the client connecting
            service.start(); // this will block until client has connected
            // TODO number of registered listeners should be 1, but this test has been flaky forever -
            // TODO CAJO library sometimes returns 2 listeners
            assertTrue(service.getNumRegisteredListeners() <= 2)

            service.sendMessage(msg)
            assertTrue(logger.containsFragment("Callback Timeout"))

        } finally {
            timeoutListener.disconnect()
            Thread.sleep(config.getRetryPeriod())
            service.stop()
            Logger.setInstance(origLogger);            
        }
    }

}
