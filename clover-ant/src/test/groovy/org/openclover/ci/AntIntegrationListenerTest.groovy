package org.openclover.ci

import org.apache.tools.ant.BuildEvent
import org.apache.tools.ant.Project
import org.junit.Test
import org.openclover.ant.tasks.AntInstrumentationConfig
import org.openclover.runtime.CloverNames

/**
 * Test for @{link AntIntegrationListener}
 */
class AntIntegrationListenerTest {

    @Test 
    void testBuildStarted() {
        AntIntegrationListener listener = new AntIntegrationListener()
        Project proj = new Project()
        proj.init()
        AntInstrumentationConfig instrConfig = new AntInstrumentationConfig(proj)
        proj.addReference(CloverNames.PROP_CONFIG, instrConfig)

        listener.buildStarted(new BuildEvent(proj))
        // Add your code here
    }

}
