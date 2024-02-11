package org.openclover.runtime.recorder;

import com_atlassian_clover.CoverageRecorder;

import java.util.HashMap;
import java.util.Map;

/**
 * A modification of GrowableCoverageRecorder which shares the same instance whenever initstring and configuration
 * bits are the same. It means that instrumentation session timestamp is ignored. Thanks to this if user has a single
 * build with multiple instrumentation sessions (typical for Grails application which compiles each domain class
 * and controller class in a separate groovyc call) only one recorder instance will be used, thus reducing number of
 * coverage files produced.
 * <p/>
 * SharedCoverageRecorder shall not be used in case when user has multiple databases with the same initstring
 * (for example: a multi-module maven project with relative initstring).
 */
public final class SharedCoverageRecorder extends GrowableCoverageRecorder {
    /**
     * Share the same coverage recorder for the same initstring+cfgbits.
     */
    static final Map<String, CoverageRecorder> sharedRecorders = new HashMap<>();

    /**
     * Factory method.
     */
    public static synchronized CoverageRecorder createFor(final String dbName, final long dbVersion, final long cfgbits, final int maxNumElements) {
        // trick: ignore dbVersion and create new instance only for new dbName+cfgbits
        final String key = dbName + "_" + cfgbits;
        if (sharedRecorders.containsKey(key)) {
            // emitting new proxy if requested maxNumElements is higher than current capacity
            final CoverageRecorder recorder = sharedRecorders.get(key).withCapacityFor(maxNumElements);
            sharedRecorders.put(key, recorder);
            return recorder;
        } else {
            final CoverageRecorder recorder = new SharedCoverageRecorder(dbName, dbVersion, cfgbits, maxNumElements).withCapacityFor(maxNumElements);
            sharedRecorders.put(key, recorder);
            return recorder;
        }
    }

    private SharedCoverageRecorder(final String dbName, final long dbVersion, final long cfgbits, final int maxNumElements) {
        super(dbName, dbVersion, cfgbits, maxNumElements, GlobalRecordingWriteStrategy.WRITE_TO_FILE);
    }

    ///CLOVER:OFF
    @Override
    public String toString() {
        return "SharedCoverageRecorder[growableRecorder=" + super.toString() + "]";
    }
    ///CLOVER:ON

}
