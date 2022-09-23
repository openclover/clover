package com.atlassian.clover.ant.tasks;

import clover.com.google.common.collect.Sets;
import com.atlassian.clover.instr.tests.TestDetector;
import com.atlassian.clover.instr.tests.TestSourceMatcher;
import com.atlassian.clover.instr.tests.NoTestDetector;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.spec.instr.test.AndSpec;
import com.atlassian.clover.spec.instr.test.BooleanSpec;
import com.atlassian.clover.spec.instr.test.OrSpec;
import com.atlassian.clover.spec.instr.test.TestClassSpec;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.util.List;
import java.util.Set;

import static clover.com.google.common.collect.Lists.newArrayList;

public class TestSourceSet extends FileSet implements TestSourceMatcher {
    private boolean enabled = true;
    private TestDetector testDetector;
    private Set<File> includedFiles = null;
    private Set<File> excludedFiles = null;
    private BooleanSpec defaultBoolSpec = new OrSpec();
    private List<BooleanSpec> boolSpecs = null;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void addConfiguredTestClass(TestClassSpec testClassSpec) {
        initBoolSpecs();
        if (!boolSpecs.contains(defaultBoolSpec)) { // add if need be
            boolSpecs.add(defaultBoolSpec);
        }
        defaultBoolSpec.addConfiguredTestClass(testClassSpec);
    }

    public void addConfiguredOr(OrSpec or) {
        initBoolSpecs();
        boolSpecs.add(or);
    }

    public void addConfiguredOrConditions(OrSpec or) {
        initBoolSpecs();
        boolSpecs.add(or);
    }

    public void addConfiguredAnd(AndSpec and) {
        initBoolSpecs();
        boolSpecs.add(and);
    }

    public void addConfiguredAndConditions(AndSpec and) {
        initBoolSpecs();
        boolSpecs.add(and);
    }

    private void initBoolSpecs() {
        if (boolSpecs == null) {
            boolSpecs = newArrayList();
        }
    }

    public void validate() throws BuildException {
        buildTestDetector();
    }

    public Set<File> getIncludedFiles() {
        maybeBuildFileSets();
        return Sets.newHashSet(includedFiles);
    }

    public Set<File> getExcludedFiles() {
        maybeBuildFileSets();
        return Sets.newHashSet(excludedFiles);
    }

    @Override
    public boolean matchesFile(File f) {
        maybeBuildFileSets();
        return includedFiles.contains(f);
    }

    @Override
    public TestDetector getDetector() {
        return testDetector;
    }

    private void buildTestDetector() {
        try {
            testDetector = enabled ? BooleanSpec.buildTestDetectorFor(boolSpecs) : new NoTestDetector();
        } catch (CloverException e) {
            throw new BuildException(e.getMessage());
        }
    }

    private void maybeBuildFileSets() {
        if (includedFiles == null) {
            Set<File> is = Sets.newHashSet();
            Set<File> es = Sets.newHashSet();

            // return empty lists if disabled; otherwise scan directory
            if (enabled) {
                final DirectoryScanner ds = getDirectoryScanner(getProject());
                File baseDir = getDir(getProject());
                String[] included = ds.getIncludedFiles();
                for (String anIncluded : included) {
                    File testFile = new File(baseDir, anIncluded);
                    is.add(testFile);
                }
                String[] excluded = ds.getExcludedFiles();
                for (String anExcluded : excluded) {
                    File testFile = new File(baseDir, anExcluded);
                    es.add(testFile);
                }
            }
            includedFiles = is;
            excludedFiles = es;
        }
    }

    @Override
    public String toString() {
        StringBuilder content = new StringBuilder("testsources(" + (getDir() != null ? "dir=" + getDir().getPath() : "") + " enabled=" + enabled);
        if (boolSpecs != null) {
            for (final BooleanSpec boolSpec : boolSpecs) {
                content.append("\n\t").append(boolSpec.toString());
            }
        }
        content.append(")");
        return content.toString();
    }
}
