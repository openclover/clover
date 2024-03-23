package org.openclover.runtime;

import org_openclover_runtime.CloverVersionInfo;

/**
 * List of system properties as well as some predefined names for created files etc.
 */
public class CloverNames {
    /** Base prefix for all Clover system properties */
    public static final String PROP_PREFIX = "clover.";
    /** Whether Clover coverage recording shall be enabled at runtime */
    public static final String PROP_ENABLE = PROP_PREFIX + "enable";
    /** Path to Clover database */
    public static final String PROP_INITSTRING = PROP_PREFIX + "initstring";
    public static final String PROP_TEST_SNAPSHOT = PROP_PREFIX + "test.snapshot";
    public static final String PROP_CONFIG = PROP_PREFIX + "config";
    /** Base directory for relative initstrings (String) */
    public static final String PROP_INITSTRING_BASEDIR = PROP_PREFIX + "initstring.basedir";
    /** File prefix to be used for initstrings (String) */
    public static final String PROP_INITSTRING_PREFIX = PROP_PREFIX + "initstring.prefix";
    /** Character which indicates a variable - a "$" by default. Used for expanding environment properties (Char) */
    public static final String PROP_ENV_PROPREF_STARTCHAR = PROP_PREFIX + "env.propref.startchar";
    /** Logging level: debug, verbose, info, warn, error (String) */
    public static final String PROP_LOGGING_LEVEL = PROP_PREFIX + "logging.level";
    /** Name of the logging adapter which shall be used (String) */
    public static final String PROP_LOGGING_ADAPTER = PROP_PREFIX + "logging.adapter";
    /** Whether to log writing/reading of tagged data files. For debugging only (Boolean) */
    public static final String PROP_LOGGING_TAGGED_IO = PROP_PREFIX + "logging.tagged.io";
    /** Name of the instrumentation profile which has to be selected at runtime (String) */
    public static final String PROP_CLOVER_PROFILE = PROP_PREFIX + "profile";

    public static final String PROP_FLUSH_EVERY_TEST = "clover.flush.everytest";
    public static final String PROP_COMPILER_DELEGATE = PROP_PREFIX + "compiler";
    public static final String PROP_INCLUDES_PATTERNSET = PROP_PREFIX + "includes";
    public static final String PROP_EXCLUDES_PATTERNSET = PROP_PREFIX + "excludes";
    /** Path to a file containing Clover license key */
    public static final String PROP_LICENSE_PATH = PROP_PREFIX + "license.path";
    public static final String PROP_CLOVER_JARPATH = PROP_PREFIX + "jarpath";
    public static final String PROP_SYNCHRONOUS_IO = PROP_PREFIX + "synchronous.io";
    public static final String PROP_RLE_COVERAGE = PROP_PREFIX + "rle.coverage";
    public static final String PROP_PER_TEST_COV = PROP_PREFIX + "pertest.coverage";
    public static final String PROP_PER_TEST_COV_THREADING = PROP_PREFIX + "pertest.coverage.threading";

    public static final String PROP_MEMORY_STRATEGY_SNAPSHOT = PROP_PREFIX + "memory.strategy.snapshot";

    public static final String PROP_CLOVER_OPTIMIZATION_ENABLED = PROP_PREFIX + "optimization.enabled";
    public static final String PROP_SKIP_TREE_MAP_REPORT = PROP_PREFIX + "skipTreeMap";

    /** Prefix used for Clover's classes, variables etc which are being added to instrumented code */
    public static final String CLOVER_PREFIX = "__CLR";
    public static final String CLOVER_RECORDER_PREFIX = CLOVER_PREFIX + CloverVersionInfo.SANITIZED_RN;
    public static final String CLOVER_TEST_NAME_SNIFFER = CloverNames.CLOVER_RECORDER_PREFIX + "_TEST_NAME_SNIFFER";

    public static final String HISTPOINT_PREFIX = "clover-";
    public static final String HISTPOINT_SUFFIX = ".xml.gz";
    public static final String SNAPSHOT_SUFFIX = ".snapshot";
    public static final String LIVEREC_SUFFIX = ".liverec";

    public static final String PROP_SERVER = PROP_PREFIX + "server";

    public static final String PROP_DISTRIBUTED_CONFIG = PROP_PREFIX + "distributed.coverage";
    public static final String CAJO_TCP_SERVER_NAME = PROP_PREFIX + "tcp.server";

    public static final String PROP_GROVER_PREFIX = PROP_PREFIX + "grover.";
    public static final String PROP_GROVER_NO_POSTBUILD_CLEANUP = PROP_GROVER_PREFIX + "no.postbuild.cleanup";

    /** Whether to dump Abstract Syntax Tree of the original and instrumented version of the Groovy class (Boolean) */
    public static final String GROVER_AST_DUMP = PROP_GROVER_PREFIX + "ast.dump";

    public static String getGroverConfigFileName() {
        return getRepkgPrefix() + "instrumentation.ser";
    }

    public static String recorderPrefixFor(int fileInfoIndex, int classInfoIndex) {
        return
            CLOVER_RECORDER_PREFIX + Integer.toString(fileInfoIndex, 36) +
            Integer.toString(classInfoIndex, 36) + Long.toString(System.currentTimeMillis(), 36);
    }

    public static final String RECORDER_FIELD_NAME = "R";

    /**
     * Work out if we are running against a repackaged Clover or the real thing
     */
    public static String getRepkgPrefix() {
        final String[] pkgTokens = CloverNames.class.getName().split("\\.");
        return pkgTokens[0].equals("org") ? "" : pkgTokens[0];
    }

    /*@NotNull*/
    public static String namespace(/*@NotNull*/ String name) {
        return "$CLV_" + name + "$";
    }
}
