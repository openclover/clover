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
     * Read the 'project.dir' system property. If not present, fallback to current working directory.
     */
    static File getProjectDirFromProperty() {
        final String projectDir = System.getProperty("project.dir")
        return projectDir != null && new File(projectDir).isDirectory() ?
                new File(projectDir) : new File().getAbsoluteFile()
    }
}
