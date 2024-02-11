package org.openclover.buildutil.testutils

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
     * Read the 'project.dir' system property. If not present, try to detect the workspace root
     * (a current working directory or it's parent).
     */
    static File getProjectDir() {
        final String projectDir = System.getProperty("project.dir")
        if (projectDir != null && new File(projectDir).isDirectory()) {
            return new File(projectDir)
        }

        File currentDir = new File(".").getAbsoluteFile().getParentFile()
        if (new File(currentDir, "CONTRIBUTING.txt").isFile()) {
            return currentDir
        }

        if (new File(currentDir.getParentFile(), "CONTRIBUTING.txt").isFile()) {
            return currentDir.getParentFile()
        }

        throw new IllegalStateException("Unable to determine project directory, project.dir=${projectDir}, current dir=${currentDir}")
    }
}
