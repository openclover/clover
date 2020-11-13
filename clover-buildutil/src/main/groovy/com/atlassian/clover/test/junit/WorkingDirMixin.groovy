package com.atlassian.clover.test.junit

/** Mixin for tests that require a working directory */
public class WorkingDirMixin {
  File workingDir;

  public void createWorkingDir() {
    workingDir = (File)File.createTempFile("clovertest", "").with { delete(); mkdirs(); it }
  }

  public void deleteWorkingDir() {
    delete(workingDir)
  }

  public void delete(File f) {
    if (f.isDirectory()) {
        f.list().each { String child-> delete(new File(f, child)) } 
    } else {
      f.delete()
    }
  }
}