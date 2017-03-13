package com.atlassian.clover.ant

import com.atlassian.clover.ant.tasks.AntInstrumentationConfig
import com.atlassian.clover.CloverNames
import com.atlassian.clover.ci.AntIntegrationListener
import org.apache.tools.ant.Project
import org.apache.tools.ant.BuildEvent
import org.junit.Test

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
