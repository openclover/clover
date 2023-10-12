package com.atlassian.clover.test.junit

/** Mixin for tests that require a working directory */
trait WorkingDirMixin {
  File workingDir

  void createWorkingDir() {
    workingDir = (File)File.createTempFile("clovertest", "").with { delete(); mkdirs(); it }
  }

  void deleteWorkingDir() {
    delete(workingDir)
  }

  void delete(File f) {
    if (f.isDirectory()) {
        f.list().each { String child-> delete(new File(f, child)) } 
    } else {
      f.delete()
    }
  }
}