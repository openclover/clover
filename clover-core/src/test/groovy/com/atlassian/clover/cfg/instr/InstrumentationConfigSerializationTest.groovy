package com.atlassian.clover.cfg.instr

import com.atlassian.clover.remote.DistributedConfig
import org.junit.Test

class InstrumentationConfigSerializationTest {

    /**
     * This simply ensures the object tree is serializable.
     * @throws IOException
     */
    @Test
    void testSerializeSanity() throws IOException {
        InstrumentationConfig config = new InstrumentationConfig()
        config.setDistributedConfig(new DistributedConfig("foo=bar"))

        File tmp = File.createTempFile("clover-test","tmp")
        tmp.deleteOnExit()
        
        config.saveToFile(tmp)
    }
}
