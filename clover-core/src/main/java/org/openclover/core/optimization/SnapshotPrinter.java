package org.openclover.core.optimization;

import org.openclover.core.cfg.Interval;
import org.openclover.core.reporters.json.JSONArray;
import org.openclover.core.reporters.json.JSONException;
import org.openclover.core.reporters.json.JSONObject;
import org.openclover.runtime.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class SnapshotPrinter {
    private static int indent;

    private static class Config {
        File outputFile; // null = System.out
        String format = "text";   // json | text
        File snapshotFile;
    }

    public static void main(String[] args) throws IOException, JSONException {

        if (args.length == 0) {
            printUsage();
        } else {
            Config config = parseCommandLineArgs(args);
            OutputStream out = (config.outputFile == null
                    ? System.out
                    : Files.newOutputStream(config.outputFile.toPath()));

            if (config.format.equalsIgnoreCase("json")) {
                jsonPrint(Snapshot.loadFrom(config.snapshotFile), out);
            } else {
                textPrint(Snapshot.loadFrom(config.snapshotFile), out);
            }

            out.flush();
            if (config.outputFile != null) {
                out.close();
            }
        }
    }

    private static Config parseCommandLineArgs(String[] args) {
        Config config = new Config();

        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-o")) {
                config.outputFile = new File(args[i+1]);
            } else if (args[i].equals("-f")) {
                config.format = args[i+1];
            }
        }

        // last param is snapshot file
        config.snapshotFile = new File(args[args.length - 1]);

        return config;
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("java " + SnapshotPrinter.class.getName() + " [-o output_file] [-f text|json] <snapshot path>");
    }

    /**
     * Print JSON output to <code>out</code> stream.
     * <pre>
     *   {
     *      "project" : {
     *          "files" : [
     *              {
     *                  "path" : "/full/path/to/source/file",
     *                  "tests" : [
     *                      "fullTestName1" : "status",
     *                      "fullTestName2" : "status",
     *                      ... more test objects ...
     *                  ]
     *              },
     *              ... more "file" objects ...
     *
     *          ]
     *
     *      }
     *   }
     * </pre>
     */
    public static void jsonPrint(Snapshot snapshot, OutputStream out) throws JSONException {
        if (snapshot != null) {
            PrintWriter writer = new PrintWriter(out);

            JSONArray jsonFilesArray = new JSONArray();
            for (Map.Entry<String, Collection<TestMethodCall>> fileEntry : snapshot.getFile2TestsMap().entrySet()) {

                JSONArray testArray = new JSONArray();
                for (TestMethodCall methodCall : fileEntry.getValue()) {
                    testArray.put(new JSONObject().put(methodCall.getRuntimeMethodName(), ""));
                }

                JSONObject jsonFileEntry = new JSONObject().put("path", fileEntry.getKey()).put("tests", testArray);
                jsonFilesArray.put(jsonFileEntry);
            }
            JSONObject jsonRoot = new JSONObject()
                    .put("project", new JSONObject()
                            .put("files", jsonFilesArray));
            writer.println(jsonRoot.toString());
            writer.flush();
        }
    }

    /**
     * Write to output stream
     */
    public static void textPrint(Snapshot snapshot, OutputStream out) {
        PrintWriter writer = new PrintWriter(out);
        if (snapshot != null) {
            writer.println("Loaded snapshot at: " + snapshot.getLocation());
            writer.println("Loaded snapshot for clover.db: " + snapshot.getInitString());
            writer.println("Snapshot's Clover version: " + snapshot.getCloverVersionInfo());
            writer.println("CLI's Clover version: " + snapshot.getCloverVersionInfo());
            writer.println("Versions: ");
            for (Long dbVersion : snapshot.getDbVersions()) {
                indent++;
                writer.println("" + dbVersion);
                indent--;
            }
            writer.println("Known tests: ");
            for (Map.Entry<String, Set<TestMethodCall>> testLookup : snapshot.getTestLookup().entrySet()) {
                indent++;
                String name = testLookup.getKey();
                Set<TestMethodCall> tests = testLookup.getValue();
                long duration = snapshot.calculateDurationOf(tests);
                writer.println(indent(name + " : duration " + durationAsString(duration)));
                for (TestMethodCall test : tests) {
                    indent++;
                    long duration2 = snapshot.calculateDurationOf(Collections.singleton(test));
                    writer.println(indent(test.toString() + " : "));
                    indent--;
                }
                indent--;
            }
            writer.flush();

            writer.println("Failing tests: ");
            for (String failingTestPath : snapshot.getFailingTestPaths()) {
                indent++;
                writer.println(indent(failingTestPath));
                indent--;
            }
            writer.flush();

            writer.println("All files: ");
            //TODO: sginter revert this functionality
            for (Map.Entry<String, Collection<TestMethodCall>> fileToTestsEntry : snapshot.getFile2TestsMap().entrySet()) {
                indent++;
                writer.println(indent(fileToTestsEntry.getKey()));
                writer.println(indent("Hitting tests:"));
                for (TestMethodCall testMethodCall : fileToTestsEntry.getValue()) {
                    indent++;
                    writer.println(indent(testMethodCall.toString()));
                    indent--;
                }
                indent--;
            }

            writer.flush();
        }
    }

    /**
     * Write to log file
     */
    public static void textPrint(Snapshot snapshot, Logger logger, int logLevel) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        textPrint(snapshot, buffer);
        try {
            logger.log(logLevel, buffer.toString(Charset.defaultCharset().name()), null);
        } catch (UnsupportedEncodingException ex) {
            logger.log(logLevel, "Unsupported encoding: " + Charset.defaultCharset().name(), ex);
        }
    }

    private static String durationAsString(long duration) {
        return
            new Interval(
                BigDecimal.valueOf(duration).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP),
                Interval.UNIT_SECOND).toSensibleString()
            + " (" + duration + " ms)";
    }

    private static String indent(String line) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            buffer.append("\t");
        }
        buffer.append(line);
        return buffer.toString();
    }
}
