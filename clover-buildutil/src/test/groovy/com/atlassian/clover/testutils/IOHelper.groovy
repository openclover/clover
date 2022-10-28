package com.atlassian.clover.testutils

class IOHelper {

    static boolean delete(File file) {
        if (file == null) {
            true
        }
        if (file.isDirectory()) {
            File[] ls = file.listFiles()
            for (File l : ls) {
                if (!delete(l)) {
                    false
                }
            }
        }
        file.delete()
    }

    static File createTmpDir(String name) throws IOException {

        // create a temporary directory.
        File tmpDir = File.createTempFile(name, "")
        tmpDir.delete()
        if (!tmpDir.mkdir()) {
            throw new RuntimeException("Unable to create temporary directory " +
                    tmpDir.getAbsolutePath())
        }
        tmpDir
    }

    /**
     * Read the 'project.dir' system property, assert that it's not null and points to a workspace directory.
     */
    static File getProjectDirFromProperty() {
        final String PROJECT_DIR = "project.dir"
        final String projectDir = System.getProperty(PROJECT_DIR)
        assertNotNull("The '" + PROJECT_DIR + "' property is not set. It must point to the Clover's workspace root",
                projectDir)
        assertTrue("The location pointed by '" + PROJECT_DIR + "' is not a directory",
                new File(projectDir).isDirectory())
        assertTrue("The location pointed by '" + PROJECT_DIR + "' does not seem to be a Clover workspace directory",
                new File(projectDir, "common.xml").isFile())

        new File(projectDir)
    }
}
