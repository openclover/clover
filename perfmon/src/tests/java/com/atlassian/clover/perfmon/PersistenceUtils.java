package org.openclover.perfmon;

import org.openclover.core.reporters.filters.DefaultTestFilter;
import org.apache.jmeter.samplers.SampleResult;

import java.io.File;
import java.io.IOException;

import org.openclover.core.registry.Clover2Registry;
import org.openclover.runtime.api.CloverException;
import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageDataSpec;
import org.openclover.core.recorder.PerTestCoverageStrategy;
import org.openclover.core.util.collections.Pair;
import org.openclover.core.cfg.StorageSize;

public class PersistenceUtils {
    public static Pair<SampleResult, CloverDatabase> sampleLoad(
        File registry,
        boolean loadCoverage,
        PerTestCoverageStrategy perTestStrategy,
        StorageSize perTestStorageSize) throws CloverException {

        final SampleResult sample = new SampleResult();
        sample.sampleStart();
        System.out.println("Loading database at " + registry.getAbsolutePath());
        final CloverDatabase db = new CloverDatabase(registry.getAbsolutePath());
        if (loadCoverage) {
            final CoverageDataSpec covSpec = new CoverageDataSpec(0l);
            covSpec.setTestFilter(new DefaultTestFilter());
            covSpec.setPerTestStrategy(perTestStrategy);
            covSpec.setPerTestStorageSize(perTestStorageSize);
            System.out.println("Loading coverage");
            db.loadCoverageData(covSpec);
        }

        sample.sampleEnd();
        sample.setSuccessful(true);
        return Pair.of(sample, db);
    }

    static SampleResult sampleSave(File registry) throws CloverException, IOException {
        final Clover2Registry reg = Clover2Registry.fromFile(registry);
        final SampleResult result = new SampleResult();
        result.sampleStart();
        System.out.println("Storing database");
        reg.saveAndOverwriteFile();
        result.sampleEnd();
        result.setSuccessful(true);
        return result;
    }
}
