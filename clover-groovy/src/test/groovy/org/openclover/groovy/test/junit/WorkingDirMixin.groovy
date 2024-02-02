package org.openclover.groovy.test.junit

import groovy.transform.CompileStatic

/** Mixin for tests that require a working directory */
@CompileStatic
trait WorkingDirMixin {
  File workingDir

  File createWorkingDir() {
    workingDir = (File)File.createTempFile("clovertest", "").with { delete(); mkdirs(); it }
    return workingDir
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