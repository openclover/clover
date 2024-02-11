package com.atlassian.clover.ant.tasks;

import com.atlassian.clover.instr.java.Instrumenter;
import com.atlassian.clover.instr.tests.FileMappedTestDetector;
import org.openclover.runtime.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.util.Set;

import static org.openclover.util.Sets.newHashSet;

/**
 *   &lt;clover-instr srcdir="" destdir="" initstring="" &gt;
 *       &lt;fileset dir="foo"&gt;
 *           &lt;include name=""/&gt;
 *       &lt;/fileset&gt;
 *   &lt;/clover-instr&gt;
 * <p/>
 *   &lt;clover-instr srcdir="" testsrcdir="" destdir="" initstring="" &gt;
 *       &lt;fileset dir="foo"&gt;
 *           &lt;include name=""/&gt;
 *       &lt;/fileset&gt;
 *       &lt;testsources dir="foo"&gt;
 *           &lt;include name=""/&gt;
 *       &lt;/testsources&gt;
 *   &lt;/clover-instr&gt;
 *
 */
public class CloverInstrTask extends AbstractInstrTask {


    private File srcDir;
    private File testSrcDir;
    private File destDir;


    public void setSrcDir(File srcDir) {
        this.srcDir = srcDir;
    }

    public void setTestSrcDir(File testSrcDir) {
        this.testSrcDir = testSrcDir;
    }

    /**
     * the destination dir for the instrumentation
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }


    @Override
    public boolean validate() {
        
        if (!super.validate()) {
            return false;
        }

        if (destDir == null) {
            throw new BuildException("destdir is required");
        }

        if (srcDir != null) {
            if (!srcDir.isDirectory()) {
                throw new BuildException("srcdir '" + srcDir + "' not found or not a directory");
            }

            if (srcDir.equals(destDir)) {
                throw new BuildException("srcdir cannot be the same as destdir");
            }

            FileSet fs = new FileSet();
            fs.setDir(srcDir);
            fs.setIncludes("**/*.java");
            addConfiguredFileSet(fs);
        }

        if (testSrcDir != null) {
            if (!testSrcDir.isDirectory()) {
                throw new BuildException("testSrcDir '" + testSrcDir + "' not found or not a directory");
            }

            if (testSrcDir.equals(destDir)) {
                throw new BuildException("testSrcDir cannot be the same as destdir");
            }

            TestSourceSet ts = new TestSourceSet();
            ts.setDir(testSrcDir);
            ts.setIncludes("**/*.java");
            addConfiguredTestSources(ts);
        }

        if ((config.getInstrFilesets() == null || config.getInstrFilesets().size() == 0) && (config.getTestSources() == null || config.getTestSources().size() == 0)) {
            throw new BuildException("You must specify either the srcdir or one or more filesets to be instrumented");
        }

        if (config.getInstrFilesets() != null) {
            for (final FileSet fileSet : config.getInstrFilesets()) {
                if (fileSet.getDir(getProject()).equals(destDir)) {
                    throw new BuildException("srcdir cannot be the same as destdir: " + destDir);
                }
            }
        }
        if (config.getTestSources() != null) {
            for (final FileSet fileSet : config.getTestSources()) {
                if (fileSet.getDir(getProject()).equals(destDir)) {
                    throw new BuildException("test srcdir cannot be the same as destdir: " + destDir);
                }
            }
        }
        return true;
    }


    @Override
    public void cloverExecute() {
        final Logger log = Logger.getInstance();
        
        try {
            final Set<File> instrSet = newHashSet();

            if (config.getInstrFilesets() != null) {
                for (final FileSet fileSet : config.getInstrFilesets()) {
                    addIncludedFilesToInstrSet(instrSet, fileSet);
                }
            }

            if (config.getTestSources() != null) {
                FileMappedTestDetector fileMappedTestDetector = new FileMappedTestDetector();

                for (final TestSourceSet testSourceSet : config.getTestSources()) {
                    testSourceSet.setProject(getProject());
                    addIncludedFilesToInstrSet(instrSet, testSourceSet);
                    fileMappedTestDetector.addTestSourceMatcher(testSourceSet);
                }

                config.setTestDetector(fileMappedTestDetector);
            }

            final Instrumenter instr = new Instrumenter(log, config);

            instr.startInstrumentation();
            for (final File file : instrSet) {
                instr.instrument(file, destDir, config.getEncoding());
            }

            instr.endInstrumentation();

        } catch (Exception e) {
            throw new BuildException(e.getMessage(), e);
        }
    }

    private void addIncludedFilesToInstrSet(Set<File> instrSet, FileSet fs) {
        final DirectoryScanner ds = fs.getDirectoryScanner(getProject());
        final File baseDir = ds.getBasedir();
        for (final String fileName : ds.getIncludedFiles()) {
            instrSet.add(new File(baseDir, fileName));
        }
    }

}
