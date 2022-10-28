package com.atlassian.clover.support

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

}
