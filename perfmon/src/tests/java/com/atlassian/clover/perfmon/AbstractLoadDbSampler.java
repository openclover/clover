package org.openclover.perfmon;

import org.apache.jmeter.config.Arguments;
import org.openclover.core.recorder.PerTestCoverageStrategy ;

public abstract class AbstractLoadDbSampler extends AbstractDbPersistenceSampler {
    protected static final String LOAD_REG_ONLY = "load.reg.only";
    protected static final String LOAD_COVERAGE = "load.coverage";
    protected static final String PER_TEST_MEMORY_STRATEGY = "pertest.mem.strategy";
    protected static final String PER_TEST_STORAGE_SIZE = "pertest.storage.size";

    @Override
    public Arguments getDefaultParameters() {
        final Arguments arguments = super.getDefaultParameters();
        arguments.addArgument(LOAD_COVERAGE, Boolean.FALSE.toString());
        arguments.addArgument(LOAD_REG_ONLY, Boolean.TRUE.toString());
        arguments.addArgument(PER_TEST_MEMORY_STRATEGY, PerTestCoverageStrategy.IN_MEMORY.name());
        arguments.addArgument(PER_TEST_STORAGE_SIZE, "256m");
        return arguments;
    }
}
