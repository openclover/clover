package org.openclover.perfmon;

import org.apache.jmeter.config.Arguments;

public abstract class AbstractDbPersistenceSampler extends AbstractCloverSampler {
    protected static final String INITSTRING = "initstring";

    @Override
    public Arguments getDefaultParameters() {
        final Arguments arguments = new Arguments();
        arguments.addArgument(INITSTRING, "${jira.workspace}/target/clover/database/clover_coverage.db");
        return arguments;
    }
}
