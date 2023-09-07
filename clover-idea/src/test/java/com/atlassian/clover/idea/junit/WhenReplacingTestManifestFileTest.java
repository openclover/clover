package com.atlassian.clover.idea.junit;

import junit.framework.TestCase;

import java.io.File;

public class WhenReplacingTestManifestFileTest extends TestCase {
    private File src;
    private File dest;

    public void testMoveFileShouldCreateNonExistingFile() throws Exception {
        //noinspection ResultOfMethodCallIgnored
        dest.delete();
        assertFalse("precondition", dest.exists());
        JUnitClassListProcessor.moveFile(src, dest);

        assertFalse(src.exists());
        assertTrue(dest.exists());
    }

    public void testMoveFileShouldOverwriteExistingDest() throws Exception {
        assertTrue("precondition", dest.exists());
        JUnitClassListProcessor.moveFile(src, dest);

        assertFalse(src.exists());
        assertTrue(dest.exists());
    }

    public void testMoveFileShouldCopeWithNonWritableDest() throws Exception {
        assertTrue("precondition", dest.setReadOnly());
        assertTrue("precondition", dest.exists());
        assertFalse("precondition", dest.canWrite());

        JUnitClassListProcessor.moveFile(src, dest);

        assertTrue(dest.canWrite());
        assertFalse(src.exists());
        assertTrue(dest.exists());
    }

    @Override
    protected void setUp() throws Exception {
        src = File.createTempFile(JUnitClassListProcessor.TMP_FILE_PREFIX, JUnitClassListProcessor.TMP_FILE_EXT);
        dest = File.createTempFile(JUnitClassListProcessor.TMP_FILE_PREFIX, JUnitClassListProcessor.TMP_FILE_EXT);

        src.deleteOnExit();
        dest.deleteOnExit();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Override
    protected void tearDown() throws Exception {
        src.delete();
        dest.delete();

    }
}
