package com.atlassian.clover.ant.tasks

import junit.framework.TestCase

import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import org.apache.tools.ant.types.Reference
import com.atlassian.clover.reporters.Current
import com.atlassian.clover.testutils.IOHelper

import static clover.com.google.common.collect.Lists.newArrayList

class FilesetFileVisitorTest extends TestCase {

    private Project proj
    private File abstractTestFile
    private File sourceFile
    private File testFile
    private List<FileSet> fileSetsWithRefId
    private FileSet fileSetReference
    private File dir

    void setUp() throws Exception {
        super.setUp()
        proj = new Project()

        //set up file directory
        dir = IOHelper.createTmpDir(getName())
        dir.deleteOnExit()
        abstractTestFile = File.createTempFile("abstracttest", "", dir)
        sourceFile = File.createTempFile("src", "", dir)
        testFile = File.createTempFile("test", "", dir)

        //set up file set using refid
        FileSet fileSetWithRefId = new FileSet()
        fileSetWithRefId.setRefid(new Reference(proj, "testsources"))
        fileSetWithRefId.setProject(proj)

        fileSetsWithRefId = newArrayList()
        fileSetsWithRefId.add(fileSetWithRefId)

        fileSetReference = new FileSet()
        proj.addReference("testsources", fileSetReference)
        fileSetReference.setProject(proj)

        fileSetReference.setDir(dir)

    }

    void tearDown() throws Exception {
        super.tearDown()
    }    

    void testCollectNoFiles() throws IOException {
        assertFileSets(proj, newArrayList(), [] as File[])
    }

    void testCollectAllFiles() throws IOException {
        assertFileSets(proj, fileSetsWithRefId, [ abstractTestFile, sourceFile, testFile ] as File[])
    }

    void testCollectTestFiles() throws IOException {
        fileSetReference.setIncludes("**/*test*")
        assertFileSets(proj, fileSetsWithRefId, [abstractTestFile, testFile] as File[])
    }

    void testCollectNonAbstractTestFiles() throws IOException {

        //set up file set for non-abstract tests
        final List<FileSet> nonAbstractFileSets = newArrayList()
        final FileSet nonAbstractFileSet = new FileSet()
        nonAbstractFileSet.setDir(dir)
        nonAbstractFileSet.setIncludes("**/*test*")
        nonAbstractFileSet.setExcludes("**/*abstracttest*")
        nonAbstractFileSets.add(nonAbstractFileSet)

        assertFileSets(proj, nonAbstractFileSets, [testFile] as File[])
    }

    private void assertFileSets(Project proj, List filesets, File[] expectedFiles) {
        final Current currentConfig = new Current()
        FilesetFileVisitor.Util.collectFiles(proj, filesets, new FilesetFileVisitor() {
            void visit(File file) {
                currentConfig.addTestSourceFile(file)
            }
        })
        List<File> expected = newArrayList(expectedFiles)
        assertEquals(expected.size(), currentConfig.getTestSourceFiles().size())
        assertEquals(expected, currentConfig.getTestSourceFiles())
    }

}
