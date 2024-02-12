package org.openclover.core.util

import org.openclover.core.TestUtils
import org.openclover.buildutil.testutils.IOHelper
import junit.framework.TestCase

import static org.openclover.core.util.Sets.newHashSet

class FileUtilsTest extends TestCase {

    protected String rootPath = "/"

    FileUtilsTest(String testName) {
        super(testName)
    }

    void setUp() throws Exception {
        // get list of roots
        final File[] roots = File.listRoots()

        // do we have uxix-style or windows-style paths?
        if (! ( (roots.length == 1) && roots[0].getAbsolutePath().equals("/") )) {
            // for windows: find a drive letter which is not used on windows - we want to avoid IOException thrown
            // from test method caused by a device which is temporary unavailable (e.g. dvd-rom or network drive)
            final Set<String> rootPaths = newHashSet()

            // collect root letters ('C', 'D'... on windows, '/' for unixes)
            for (File root : File.listRoots()) {
                rootPaths.add(root.getAbsolutePath())
            }
            // find first unused
            for (char c = 'A'; c < 'Z'; c++) {
                if ( ! rootPaths.contains("$c:\\") ) {
                    this.rootPath = "$c:\\"
                    break
                }
            }
        }
    }

    void tearDown() throws Exception {

    }

    void testRelativePath() throws IOException {

        // check a simple parent path
        test(new File(rootPath + "a/b/c"), new File(rootPath + "a/b/d.file"))

        // check a simple child path
        test(new File(rootPath + "a/b"), new File(rootPath + "a/b/c/d.file"))

        // check a combined parent - child path
        test(new File(rootPath + "a/b/d"), new File(rootPath + "a/b/c/d.file"))

        // include a space in the directory name
        test(new File(rootPath + "a/b/x d"), new File(rootPath + "a/b/c/d.file"))

        test(new File(rootPath + "a/b/c/e/f"), new File(rootPath + "a/l/m/n/d.file"))

        test(new File(rootPath + "a/b/c/e/f"), new File(rootPath + "a/e/f/g/d.file"))
    }

    private void test(File a, File b) throws IOException {
        String relative = FileUtils.getRelativePath(a, b)
        assertEquals(b.getCanonicalPath(), new File(a, relative).getCanonicalPath())
    }

    void testCreateTempDir() throws IOException
    {
        // test when parent dir does not exist
        File tmpDir = IOHelper.createTmpDir(getName())
        FileUtils.createTempDir(getName(), new File(tmpDir, "blah"))
    }

    /**
     * @see FileUtils#getInstance()
     */
    void testGetInstance() {
        FileUtils utils = FileUtils.getInstance()
        assertNotNull(utils)
    }

    /**
     * @see FileUtils#getNormalizedPath(String)
     */
    void testGetNormalizedPath() {
        // windows, unix, mixture to normalized
        assertEquals("c:/windows/path", FileUtils.getNormalizedPath("c:\\windows\\path"))
        assertEquals("/unix/path", FileUtils.getNormalizedPath("/unix/path"))
        assertEquals("/some/weird/mixture/path", FileUtils.getNormalizedPath("\\some\\weird/mixture/path"))
        // null
        assertEquals(null, FileUtils.getNormalizedPath(null))
    }

    /**
     * @see FileUtils#getPlatformSpecificPath(String)
     */
    void testGetPlatformSpecificPath() {
        // windows, unix, mixture to current os
        final char sep = File.separatorChar
        assertEquals("c:" + sep + "windows" + sep + "path", FileUtils.getPlatformSpecificPath("c:\\windows\\path"))
        assertEquals("${sep}unix${sep}path".toString(), FileUtils.getPlatformSpecificPath("/unix/path"))
        assertEquals("${sep}some${sep}weird${sep}mixture${sep}path".toString(), FileUtils.getPlatformSpecificPath("\\some\\weird/mixture/path"))
        // null
        assertEquals(null, FileUtils.getPlatformSpecificPath(null))
    }

    /**
     * Test copying of files only with no removal of target directory.
     * @see FileUtils#dirCopy(java.io.File, java.io.File, boolean)
     */
    void testDirCopyFlatNoDelete() throws IOException {
        File destDir = setUpEmptyDir()
        File srcDir1 = setUpFlatDir("1st_")
        File srcDir2 = setUpFlatDir("2nd_")

        // we expect three files after first copy
        FileUtils.dirCopy(srcDir1, destDir, false)
        assertEquals(3, destDir.list().length)

        // we expect six files (from two source directories) after copy
        FileUtils.dirCopy(srcDir2, destDir, false)
        assertEquals(6, destDir.list().length)

        // modify one file in target directory and check if will be overwritten
        File modifiedFile = new File(destDir, "2nd_one.java")
        PrintWriter writer = new PrintWriter(new FileOutputStream(modifiedFile))
        writer.print("Hello!")
        writer.close()
        // we expect still six files after copy
        FileUtils.dirCopy(srcDir2, destDir, false)
        assertEquals(6, destDir.list().length)
        // we expect empty file as it was overwritten
        LineNumberReader reader = new LineNumberReader(new FileReader(modifiedFile))
        String line = reader.readLine()
        reader.close()
        assertNull(line)
    }

    /**
     * Test copying of files only with removal of target directory.
     * @see FileUtils#dirCopy(java.io.File, java.io.File, boolean)
     */
    void testDirCopyFlatWithDelete() throws IOException {
        File destDir = setUpEmptyDir()
        File srcDir1 = setUpFlatDir("1st_")
        File srcDir2 = setUpFlatDir("2nd_")

        // we expect three files after first copy
        FileUtils.dirCopy(srcDir1, destDir, true)
        assertEquals(3, destDir.list().length)
        assertTrue(new File(destDir, "1st_one.java").exists())

        // we expect three files (from second source directory only) after copy
        FileUtils.dirCopy(srcDir2, destDir, true)
        assertEquals(3, destDir.list().length)
        assertFalse(new File(destDir, "1st_one.java").exists())
        assertTrue(new File(destDir, "2nd_one.java").exists())
    }

    /**
     * Test copying of files and directories with no removal of target directory.
     * @see FileUtils#dirCopy(java.io.File, java.io.File, boolean)
     */
    void testDirCopyRecursiveNoDelete() throws IOException {
        File destDir = setUpEmptyDir()
        File srcDir1 = setUpTreeDir("1st_")
        File srcDir2 = setUpTreeDir("2nd_")

        // we expect three dirs after copy in top-level
        FileUtils.dirCopy(srcDir1, destDir, false)
        String firstOne22 = destDir.getAbsolutePath() + File.separator + "1st_one" + File.separator + "1st_one2" + File.separator + "1st_one22"
        assertEquals(3, destDir.list().length)
        assertTrue(new File(destDir, "1st_one").isDirectory())
        assertTrue(new File(firstOne22).isDirectory())

        // we expect even more after second copy
        FileUtils.dirCopy(srcDir2, destDir, false)
        String secondOne22 = destDir.getAbsolutePath() + File.separator + "2nd_one" + File.separator + "2nd_one2" + File.separator + "2nd_one22"
        assertEquals(6, destDir.list().length)
        assertTrue(new File(destDir, "1st_one").isDirectory())
        assertTrue(new File(destDir, "2nd_one").isDirectory())
        assertTrue(new File(firstOne22).isDirectory())
        assertTrue(new File(secondOne22).isDirectory())
    }

    /**
     * Test copying of files and directories with removal of target directory.
     * @see FileUtils#dirCopy(java.io.File, java.io.File, boolean)
     */
    void testDirCopyRecursiveWithDelete() throws IOException {
        File destDir = setUpEmptyDir()
        File srcDir1 = setUpTreeDir("1st_")
        File srcDir2 = setUpTreeDir("2nd_")

        // we expect three dirs after copy in top-level
        FileUtils.dirCopy(srcDir1, destDir, true)
        String firstOne22 = destDir.getAbsolutePath() + File.separator + "1st_one" + File.separator + "1st_one2" + File.separator + "1st_one22"
        assertEquals(3, destDir.list().length)
        assertTrue(new File(destDir, "1st_one").isDirectory())
        assertTrue(new File(firstOne22).isDirectory())

        // we expect only stuff from second source dir
        FileUtils.dirCopy(srcDir2, destDir, true)
        String secondOne22 = destDir.getAbsolutePath() + File.separator + "2nd_one" + File.separator + "2nd_one2" + File.separator + "2nd_one22"
        assertEquals(3, destDir.list().length)
        assertFalse(new File(destDir, "1st_one").isDirectory())
        assertTrue(new File(destDir, "2nd_one").isDirectory())
        assertFalse(new File(firstOne22).isDirectory())
        assertTrue(new File(secondOne22).isDirectory())
    }

    /**
     * Test dirCopy with non-existing source dir.
     * @see FileUtils#dirCopy(java.io.File, java.io.File, boolean)
     */
    void testDirCopyValidateSourceDirectory() {
        // source is not a directory
        try {
            FileUtils.dirCopy(new File("/source/file/which/is/not/a/directory"), null, false)
            fail("Expected exception on /source/file/which/is/not/a/directory")
        } catch (IOException ex) {
            assertTrue(ex.getMessage().contains("is not a directory"))
        }
    }

    /**
     * Test dirCopy with source dir being a parent of target.
     * @see FileUtils#dirCopy(java.io.File, java.io.File, boolean)
     */
    void testDirCopyValidateTargetIsSubdirOfSource() {
        // destination is a subdirectory of source
        try {
            File tmpDir = FileUtils.getJavaTempDir()
            File parentDir = new File(tmpDir, "parent")
            File childDir = new File(parentDir, "child")
            parentDir.mkdir()

            FileUtils.dirCopy(parentDir, childDir, false)
            fail("Expected exception on copy into subdir")
        } catch (IOException ex) {
            assertTrue(ex.getMessage().contains("is sub-directory"))
        }
    }

    /**
     * Test dirCopy with target dir being same as source.
     * @see FileUtils#dirCopy(java.io.File, java.io.File, boolean)
     */
    void testDirCopyValidateTargetIsSameAsSource() {
        // destination is the same as source
        try {
            File tmpDir = FileUtils.getJavaTempDir()
            // two instances to ensure that we compare paths, not object references
            File sourceDir = new File(tmpDir, "source")
            File targetDir = new File(tmpDir, "source")
            sourceDir.mkdir()

            FileUtils.dirCopy(sourceDir, targetDir, false)
            fail("Expected exception on target==source")
        } catch (IOException ex) {
            assertTrue(ex.getMessage().contains("is same as source"))
        }
    }

    /**
     * Creates an empty directory in java.io.tmpdir.
     * @return File - created directory
     */
    private File setUpEmptyDir() throws IOException {
        File dir = TestUtils.createEmptyDirFor(getClass(), "FileUtilsTest_EmptyDir")

        // make sure that it's empty
        FileUtils.deltree(dir)
        dir.mkdirs()
        assertTrue("Failed setUpEmptyDir on '" + dir + "' isDirectory=" + dir.isDirectory() + " isEmpty=" + (dir.list().length == 0),
                dir.isDirectory())
        return dir
    }

    /**
     * Creates a flat directory (with files only) in java.io.tmpdir.
     * @param prefix prefix for file names
     * @return File - created directory
     */
    private File setUpFlatDir(String prefix) throws IOException {
        String[] fileNames = [ "one.java", "two.java", "three.java" ]
        File dir = TestUtils.createEmptyDirFor(getClass(), "FileUtilsTest_FlatDir_" + prefix)

        // create files in it
        try {
            for (String fileName : fileNames) {
                File f = new File(dir, prefix + fileName)
                boolean created = f.createNewFile()
                assertTrue("Failed setUpFlatDir on '" + f + "' created=" + created, created)
            }
        } catch (IOException ex) {
            fail("Failed setUpFlatDir: " + ex.toString())
        }
        return dir
    }


    /**
     * Creates a directory with subdirectories and files in java.io.tmpdir.
     * @param prefix prefix for file/directory names
     * @return File - created directory
     */
    private File setUpTreeDir(String prefix) throws IOException {
        // layout:
        // /one             - dir with file and subdirs
        //     /one.java
        //     /one1        - two level of subdirs
        //     /one2
        //          /one22  - three levels of subdirs
        // /two             - dir with a file
        //     /two.java
        // /three           - empty dir
        File dir = TestUtils.createEmptyDirFor(getClass(), "FileUtilsTest_TreeDir_" + prefix)

        // create structure in it
        try {
            File dirOne = new File(dir, prefix + "one")
            File dirOne1 = new File(dirOne, prefix + "one1")
            File dirOne2 = new File(dirOne, prefix + "one2")
            File dirOne22 = new File(dirOne2, prefix + "one22")
            File dirTwo = new File(dir, prefix + "two")
            File dirThree = new File(dir, prefix + "three")

            File fileOne = new File(dirOne, prefix + "one.java")
            File fileTwo = new File(dirTwo, prefix + "two.java")

            boolean dirResult = (dirOne.mkdir() && dirOne1.mkdir() && dirOne2.mkdir() && dirOne22.mkdir() && dirTwo.mkdir() && dirThree.mkdir())
            assertTrue("Failed setUpTreeDir on sub-directory creation", dirResult)

            boolean fileResult = (fileOne.createNewFile() && fileTwo.createNewFile())
            assertTrue("Failed setUpTreeDir on file creation", fileResult)
        } catch (IOException ex) {
            fail("Failed setUpTreeDir: " + ex.toString())
        }

        // return dir
        return dir
    }
}