package org.openclover.idea.junit;

import org.openclover.core.api.optimization.Optimizable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class WhenJUnitClassListProcessorReadsOptimizablesTest {
    private File tmpFile;

    @Test
    public void testShouldReadValidFiles() throws Exception {
        for (String[] testCase : FileBasedJUnitClassListProcessorIdeaTest.TEST_CASES) {
            fillTmpFile(testCase);
            List<String> header = newArrayList();
            final List<Optimizable> optimizables = JUnitClassListProcessor.readOptimizables(tmpFile, header);


            assertEquals(testCase[0], header.get(0));
            assertEquals(testCase[1], header.get(1));

            assertNotNull(optimizables);
            assertEquals(testCase.length - 2, optimizables.size());
            final Iterator<Optimizable> optimizableIterator = optimizables.iterator();
            for (int i = 2; i < testCase.length; i++) {
                assertEquals(testCase[i], optimizableIterator.next().getName());
            }
        }
    }

    @Test
    public void testShouldReadValidFilesIdea11To13() throws Exception {
        for (String[] testCase : FileBasedJUnitClassListProcessorIdeaTest.TEST_CASES_IDEA_11_TO_13) {
            fillTmpFile(testCase);
            List<String> header = newArrayList();
            final List<Optimizable> optimizables = JUnitClassListProcessor.readOptimizables(tmpFile, header);

            assertEquals(testCase[0], header.get(0));

            assertNotNull(optimizables);
            assertEquals(testCase.length -1, optimizables.size());
            final Iterator<Optimizable> optimizableIterator = optimizables.iterator();
            for (int i = 1; i < testCase.length; i++) {
                assertEquals(testCase[i], optimizableIterator.next().getName());
            }
        }
    }

    @Test
    public void testShouldReadValidFilesIdea14AndAbove() throws Exception {
        for (String[] testCase : FileBasedJUnitClassListProcessorIdeaTest.TEST_CASES_IDEA_14_AND_ABOVE) {
            fillTmpFile(testCase);
            final List<String> header = newArrayList();
            final List<Optimizable> optimizables = JUnitClassListProcessor.readOptimizables(tmpFile, header);

            assertNotNull(optimizables);
            for (int i = 0; i < 2; i++) {
                assertEquals(testCase[i], header.get(i));
            }

            assertEquals(testCase.length - 2, optimizables.size());
            final Iterator<Optimizable> optimizableIterator = optimizables.iterator();
            for (int i = 2; i < testCase.length; i++) {
                assertEquals(testCase[i], optimizableIterator.next().getName());
            }
        }
    }

    @Test
    public void testShouldCopeWithEmptyFiles() throws Exception {
        List<String> header = newArrayList();
        assertNull(JUnitClassListProcessor.readOptimizables(tmpFile, header));
    }

    @Test
    public void testShouldCopeWithInvalidFiles() throws Exception {
        final String[] data = {"-junit3"};
        fillTmpFile(data);

        List<String> header = newArrayList();
        assertNull(JUnitClassListProcessor.readOptimizables(tmpFile, header));
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

    private void fillTmpFile(String[] data) throws IOException {
        FileBasedJUnitClassListProcessorIdeaTest.fillTmpFile(tmpFile, data);
    }
}
