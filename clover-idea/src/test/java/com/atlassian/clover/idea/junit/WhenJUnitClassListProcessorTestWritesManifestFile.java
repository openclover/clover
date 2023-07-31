package com.atlassian.clover.idea.junit;

import com.atlassian.clover.api.optimization.Optimizable;
import com.atlassian.clover.api.optimization.StringOptimizable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.openclover.util.Lists.newArrayList;
import static org.junit.Assert.*;

public class WhenJUnitClassListProcessorTestWritesManifestFile {
    private File tmpFile;


    @Test
    public void testShouldReadBackAsExpected() throws Exception {
        for (String[] testCase : FileBasedJUnitClassListProcessorIdeaTest.TEST_CASES) {
            List<String> header = Arrays.asList(testCase[0], testCase[1]);
            final List<Optimizable> optimizables = new ArrayList<>(testCase.length - 2);
            for (int i = 2; i < testCase.length; i++) {
                optimizables.add(new StringOptimizable(testCase[i]));
            }

            assertTrue("precondition", tmpFile.delete());
            assertTrue("precondition", tmpFile.createNewFile());

            assertTrue(JUnitClassListProcessor.writeManifestFile(tmpFile, header.toArray(new String[header.size()]), optimizables));
            List<String> newHeader = newArrayList();
            final List<Optimizable> newOptimizables = JUnitClassListProcessor.readOptimizables(tmpFile, newHeader);

            assertEquals(header, newHeader);
            assertEquals(asStrings(optimizables), asStrings(newOptimizables));
        }
    }

    @Test
    public void testShouldReadBackAsExpectedIdea11AndAbove() throws Exception {
        for (String[] testCase : FileBasedJUnitClassListProcessorIdeaTest.TEST_CASES_IDEA_11_TO_13) {
            List<String> header = Arrays.asList(testCase[0]);
            final List<Optimizable> optimizables = new ArrayList<>(testCase.length - 1);
            for (int i = 1; i < testCase.length; i++) {
                optimizables.add(new StringOptimizable(testCase[i]));
            }

            assertTrue("precondition", tmpFile.delete());
            assertTrue("precondition", tmpFile.createNewFile());

            assertTrue(JUnitClassListProcessor.writeManifestFile(tmpFile,
                    header.toArray(new String[header.size()]), optimizables));
            List<String> newHeader = new ArrayList<>();
            final List<Optimizable> newOptimizables = JUnitClassListProcessor.readOptimizables(tmpFile, newHeader);

            assertEquals(header, newHeader);
            assertEquals(asStrings(optimizables), asStrings(newOptimizables));
        }
    }

    private static List<String> asStrings(List<Optimizable> optimizables) {
        final List<String> strings = new ArrayList<>(optimizables.size());
        for (Optimizable optimizable : optimizables) {
            strings.add(optimizable.getName());
        }

        return strings;
    }


    @Before
    public void setUp() throws Exception {
        tmpFile = File.createTempFile(JUnitClassListProcessor.TMP_FILE_PREFIX, JUnitClassListProcessor.TMP_FILE_EXT);
        tmpFile.deleteOnExit();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @After
    public void tearDown() throws Exception {
        tmpFile.delete();
    }

}
