package com.atlassian.clover.instr.tests;


import java.io.File;
import java.io.Serializable;


public interface TestSourceMatcher extends Serializable {

    public boolean matchesFile(File f);

    public TestDetector getDetector();

}
