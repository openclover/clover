package com.atlassian.clover.support;

import java.io.File;
import java.io.IOException;

public class IOHelper {

    public static boolean delete(File file) {
        if (file == null) {
            return true;
        }
        if (file.isDirectory()) {
            File[] ls = file.listFiles();
            for (File l : ls) {
                if (!IOHelper.delete(l)) {
                    return false;
                }
            }
        }
        return file.delete();
    }

    public static File createTmpDir(String name)
            throws IOException {

        // create a temporary directory.
        File tmpDir = File.createTempFile(name, "");
        tmpDir.delete();
        if (!tmpDir.mkdir()) {
            throw new RuntimeException("Unable to create temporary directory " +
                    tmpDir.getAbsolutePath());
        }
        return tmpDir;
    }

    

}
