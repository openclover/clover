package org.openclover.perfmon;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.io.File;

import org.openclover.core.CloverDatabase;
import org.openclover.runtime.api.CloverException;
import org.openclover.core.recorder.PerTestCoverageStrategy;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.core.cfg.StorageSize;

public class QueryPerTestCoverageHitsHeapSampler extends LoadDbHeapSampler {
    @Override
    protected CloverDatabase load(JavaSamplerContext context) throws CloverException {
        final CloverDatabase db = PersistenceUtils.sampleLoad(
            new File(context.getParameter(INITSTRING)),
            Boolean.valueOf(context.getParameter(LOAD_COVERAGE)),
            PerTestCoverageStrategy.valueOf(context.getParameter(PER_TEST_MEMORY_STRATEGY)),
            StorageSize.fromString(context.getParameter(PER_TEST_STORAGE_SIZE))).second;

        //Query per-test coverage which should fill the cache if cache is in use
        for (TestCaseInfo tci : db.getCoverageData().getTests()) {
            db.getCoverageData().getHitsFor(tci);
        }

        return db;
    }
}
