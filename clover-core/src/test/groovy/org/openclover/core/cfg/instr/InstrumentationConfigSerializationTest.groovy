package org.openclover.core.cfg.instr

import org.junit.Test
import org.openclover.runtime.remote.DistributedConfig

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
