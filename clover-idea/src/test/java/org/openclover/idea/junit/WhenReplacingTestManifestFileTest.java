package org.openclover.idea.junit;

import junit.framework.TestCase;
import org.junit.Assume;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

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
        // Use POSIX permissions directly — File.setReadOnly() + canWrite() is unreliable on macOS
        // (ACLs on temp dirs can allow writes even after setReadOnly()).
        Files.setPosixFilePermissions(dest.toPath(), EnumSet.of(PosixFilePermission.OWNER_READ));
        assertTrue("precondition", dest.exists());
        // Skip on filesystems without POSIX permission support (e.g. exFAT) where the call above is a no-op.
        Assume.assumeFalse("POSIX permissions not enforced on this filesystem", dest.canWrite());

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
