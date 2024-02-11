package com.atlassian.clover.util;

import org.openclover.runtime.CloverNames;
import org.openclover.runtime.Logger;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.optimization.Snapshot;
import com_atlassian_clover.CloverVersionInfo;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openclover.util.Sets.newHashSet;

public class CloverUtils {

    /**
     * An object to synchronize on when creating directories on the File System.
     */
    private static final Object FILE_SYSTEM_LOCK = new Object();
    public static final String SYNTH_TESTNAME_PREF = CloverNames.CLOVER_PREFIX + CloverVersionInfo.SANITIZED_RN;

    private static final HashSet<String> IGNORE_TRACE_LINES = newHashSet(
            "sun.reflect.NativeMethodAccessorImpl.invoke",
            "sun.reflect.DelegatingMethodAccessorImpl.invoke",
            "sun.reflect.NativeMethodAccessorImpl.invoke0",
            "java.lang.reflect.Method.invoke",
            "junit.framework.Assert.assertEquals",
            "junit.framework.TestCase.runTest",
            "junit.framework.TestCase.runBare",
            "junit.framework.TestResult$1.protect",
            "junit.framework.TestResult.runProtected",
            "junit.framework.TestResult.run",
            "junit.framework.TestCase.run",
            "junit.framework.TestSuite.runTest",
            "junit.framework.TestSuite.run",
            "org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.run",
            "org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.launch",
            "org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main"
    );


    /**
     * Deletes the coverage recordings, and optionally the database itself
     *
     * @return true if successful.
     */
    public static boolean scrubCoverageData(String initString, final boolean deleteDatabase) {
       return scrubCoverageData(initString, deleteDatabase, false, false);
    }

    public static boolean scrubCoverageData(String initString, final boolean deleteDatabase, final boolean deleteSnapshot, boolean verbose) {
        if (initString == null) {
            return false;
        }

        File cloverDb = new File(initString);
        File cloverDbDir = cloverDb.getParentFile();
        // just a little bit defensive :-)
        if (cloverDbDir != null && cloverDbDir.exists() && cloverDbDir.isDirectory()) {
            final String baseName = cloverDb.getName();
            final String testSnapshotName = Snapshot.fileNameForInitString(baseName);
            final String[] fileNames = cloverDbDir.list((dir, name) -> {
                if (baseName.equals(name)) {
                    return deleteDatabase;
                } else if (testSnapshotName.equals(name)) {
                    return deleteSnapshot;
                } else {
                    return name.startsWith(baseName);
                }
            });

            if (fileNames != null && fileNames.length > 0) {
                for (final String fileName : fileNames) {
                    final File covFile = new File(cloverDbDir, fileName);
                    if (covFile.exists() && !covFile.isDirectory()) {
                        if (verbose) {
                            Logger.getInstance().info("Deleting " + covFile.getAbsolutePath());
                        }
                        if (!covFile.delete()) {
                            // problem deleting one of the files.
                            Logger.getInstance().warn("Unable to delete " + covFile.getAbsolutePath());
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * Converts a class, including inner classes and anon. inner classes to
     * a class name that Clover can use when looking up coverage information.
     * Basically it's a hack - see CCD-294, CCD-307
     */
    public static String cloverizeClassName(String className) {
        return className == null ? className : className.replace('$', '.').replaceAll("\\.[0-9]+", "");
    }

    public static File createOutFile(FullFileInfo finfo, String outname, File baseDir) throws IOException {
        PackageInfo pkg = finfo.getContainingPackage();
        File outdir = createOutDir(pkg, baseDir);
        return new File(outdir, outname);
    }

    public static File createOutDir(PackageInfo pkg, File basePath) throws IOException {
        File outdir = new File(basePath, pkg.isDefault() ? pkg.getName() : pkg.getPath());
        createDir(outdir);
        return outdir;
    }

    public static void createDir(File outdir) throws IOException {
        synchronized (FILE_SYSTEM_LOCK) {
            if (!outdir.mkdirs() && !outdir.isDirectory()) {
                throw new IOException("Failed to create directory " + outdir);
            }
        }
    }

    public static void logDeprecationWarning(String name) {
        Logger.getInstance().warn("WARNING: The " + name + " is deprecated. It will be ignored.");
    }

    public static String createSyntheticTestName(MethodInfo method) {
        return SYNTH_TESTNAME_PREF +
                Long.toString(Math.abs(method.getName().hashCode()), 36) +
                Integer.toString(method.getDataIndex(), 36);
    }


    /**
     * Transforms the linenumbers of a synthetic test method name, with the method on the next line.
     * The synthetic test line is removed from the text.
     * @param trace the stack trace to transform the line numbers
     * @return the transformed text
     */
    public static String transformStackTrace(String trace, boolean filterTrace) {
        if (trace == null) {
            return null;
        }

        final String FILE_REGEX = "(\\(.*\\))";
        final String SYNTH_NAME_REGEXP = SYNTH_TESTNAME_PREF;

        final Pattern synthPattern = Pattern.compile(SYNTH_NAME_REGEXP);
        final Pattern filePattern = Pattern.compile(FILE_REGEX);

        final StringBuilder buff = new StringBuilder();
        final char nl = '\n';

        // get each line of the trace
        LineNumberReader reader = new LineNumberReader(new StringReader(trace));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                final Matcher synthMatcher = synthPattern.matcher(line);


                if (synthMatcher.find()) {
                    // remove synth line, keeping the file part of course
                    // get the file part
                    final Matcher fileMatcher = filePattern.matcher(line);
                    if (fileMatcher.find()) {
                        // found the file part of the synth line
                        final String filePart = fileMatcher.group(1);
                        // get the next line
                        final String nextLine = reader.readLine();
                        if (nextLine == null) {
                            buff.append(line);
                            return buff.toString();
                        }
                        // remove the file part of the next line
                        Matcher nextMatcher = filePattern.matcher(nextLine);
                        if (nextMatcher.find()) {
                            // append start of this line
                            buff.append(nextLine.subSequence(0, nextMatcher.start()));
                            buff.append(filePart);
                            buff.append(nl);
                        } else { // next line has no file part
                            // use synth line
                            buff.append(line);
                            buff.append(nl);
                            buff.append(nextLine);
                            buff.append(nl);
                        }

                    } else { // no file part on the synthetic line
                        // just append the synth line
                        buff.append(line);
                        buff.append(nl);
                    }

                } else {
                    if (filterTrace) {
                        // possibly filter this line if it is a framework line
                        int firstSpace = line.indexOf(" ");
                        int openParen = line.indexOf("(");
                        if (firstSpace >= 0 && openParen >= 0 && firstSpace < openParen) {
                            String fqmn = line.substring(firstSpace + 1, openParen);
                            if (IGNORE_TRACE_LINES.contains(fqmn)) {
                                continue;
                            }
                        }
                    }
                    // add the line to the buffer
                    buff.append(line);
                    buff.append(nl);
                }
            }
        } catch (IOException e) {
            Logger.getInstance().error(e.getMessage() + " reading stack trace: " + trace);
        }

        return buff.toString().trim();
    }

    public static String packageNameToPath(String name, boolean isDefaultPkg) {
        String workingPath = "";
        if (!isDefaultPkg && name.length() > 0) {
            workingPath = name.replace('.', '/') + '/';
        }
        return workingPath;
    }
}
