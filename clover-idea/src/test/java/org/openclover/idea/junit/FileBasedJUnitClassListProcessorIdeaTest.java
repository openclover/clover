package org.openclover.idea.junit;

import com.atlassian.clover.api.optimization.Optimizable;
import org.openclover.idea.junit.config.OptimizedConfigurationSettings;
import com.atlassian.clover.optimization.OptimizationSession;
import org.openclover.runtime.util.IOStreamUtils;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightIdeaTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.openclover.util.Lists.newArrayList;

public class FileBasedJUnitClassListProcessorIdeaTest extends LightIdeaTestCase {
    static final String[][] TEST_CASES = {
            {"-junit3", ""},
            {"-junit3", "package.name1"},
            {"-junit3", "", "com.atlassian.clover.Test1"},
            {"-junit3", "", "com.atlassian.clover.Test1", "com.atlassian.clover.Test2"},
            {"-junit3", "package.name", "com.atlassian.clover.Test1", "com.atlassian.clover.Test2"},
    };

    static final String[][] TEST_CASES_IDEA_11_TO_13 = {
            {""},
            {"package.name1"},
            {"", "com.atlassian.clover.Test1"},
            {"", "com.atlassian.clover.Test1", "com.atlassian.clover.Test2"},
            {"package.name", "com.atlassian.clover.Test1", "com.atlassian.clover.Test2"},
    };

    static final String[][] TEST_CASES_IDEA_14_AND_ABOVE = {
            {"", ""},
            {"", "", "com.atlassian.clover.Test1"},
            {"", "", "com.atlassian.clover.Test1", "com.atlassian.clover.Test2"},
    };

    private static final SavingsReporter DUMMY_SAVINGS_REPORTER = (project, optimizationSession) -> { };

    public void testProcessExisting() throws Exception {
        for (String[] testCase : TEST_CASES) {
            if (testCase.length < 3) {
                continue; // tests fail to load message bundle for some reason
            }
            final File origFile = File.createTempFile(JUnitClassListProcessor.TMP_FILE_PREFIX, JUnitClassListProcessor.TMP_FILE_EXT);
            origFile.deleteOnExit();
            fillTmpFile(origFile, testCase);

            final FileBasedJUnitClassListProcessor processor = new TestedProcessor(origFile, DUMMY_SAVINGS_REPORTER, getProject());
            assertNull(processor.processWhenFileNotEmpty());
            assertEquals(Arrays.asList(testCase), readTmpFile(origFile));

            origFile.deleteOnExit();
        }
    }

    public void testProcessDelayed() throws Exception {
        for (final String[] testCase : TEST_CASES) {
            if (testCase.length < 3) {
                continue; // tests fail to load message bundle for some reason
            }
            final File origFile = File.createTempFile(JUnitClassListProcessor.TMP_FILE_PREFIX, JUnitClassListProcessor.TMP_FILE_EXT);
            origFile.deleteOnExit();

            final Thread asyncWriter = new Thread("Async manifest file creator") {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    try {
                        fillTmpFile(origFile, testCase);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            };

            asyncWriter.start();

            final FileBasedJUnitClassListProcessor processor = new TestedProcessor(origFile, DUMMY_SAVINGS_REPORTER, getProject());

            final File newFile = processor.processWhenFileNotEmpty();
            assertNotNull(newFile);
            assertTrue(newFile.exists());
            assertEquals(Arrays.asList(testCase), readTmpFile(newFile));
            //noinspection ResultOfMethodCallIgnored
            newFile.delete();
        }
    }

    //FLAKEY TEST
//    public void testProcessNonAtomicFlush() throws Exception {
//        final int OPTIMIZABLE_COUNT = 100;
//
//        final File origFile = File.createTempFile(JUnitClassListProcessor.TMP_FILE_PREFIX, JUnitClassListProcessor.TMP_FILE_EXT);
//        origFile.deleteOnExit();
//
//        final Thread asyncWriter = new Thread("Async manifest file creator with non-atomic flush") {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(100);
//                    final String[] header = {"-junit3", "nonatomic"};
//                    fillTmpFile(origFile, header);
//                    final String[] data = new String[1];
//                    for (int i = 0; i < OPTIMIZABLE_COUNT; i++) {
//                        Thread.sleep(10);
//                        data[0] = "com.atlassian.clover.Class" + i;
//                        fillTmpFile(origFile, data, true);
//                    }
//
//                } catch (InterruptedException e1) {
//                    e1.printStackTrace();
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
//            }
//        };
//
//        asyncWriter.start();
//
//        final FileBasedJUnitClassListProcessor processor = new TestedProcessor(origFile, DUMMY_SAVINGS_REPORTER, getProject());
//
//        final File newFile = processor.processWhenFileNotEmpty();
//        assertNotNull(newFile);
//        assertTrue(newFile.exists());
//        assertEquals(2 + OPTIMIZABLE_COUNT, readTmpFile(newFile).size());
//        //noinspection ResultOfMethodCallIgnored
//        newFile.delete();
//    }

    public static void fillTmpFile(File tmpFile, String[] data) throws IOException {
        fillTmpFile(tmpFile, data, false);
    }

    public static void fillTmpFile(File tmpFile, String[] data, boolean append) throws IOException {
        final PrintWriter out = new PrintWriter(new FileWriter(tmpFile, append)); // exactly as TestObject does
        for (String line : data) {
            out.println(line);
        }

        out.close();
    }

    public static List<String> readTmpFile(File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            final List<String> contents = newArrayList();
            for (String s = reader.readLine(); s != null; s = reader.readLine()) {
                contents.add(s);
            }
            return contents;
        } finally {
            IOStreamUtils.close(reader);
        }
    }

    private class TestedProcessor extends FileBasedJUnitClassListProcessor {

        TestedProcessor(File ideaGeneratedFile, SavingsReporter savingsReporter, Project currentProject) {
            super(savingsReporter, ideaGeneratedFile, currentProject, null);
        }

        @NotNull
        @Override
        Collection<Optimizable> optimize(@NotNull Project project, @NotNull OptimizedConfigurationSettings settings, @NotNull List<Optimizable> optimizables, OptimizationSession[] sessionHolder) {
            return optimizables;
        }
    }
}
