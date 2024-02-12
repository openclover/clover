package org.openclover.perfmon;

import java.io.File;
import java.io.IOException;

public class InstrumentationUtils {
    static File makeTempDir(String prefix, String suffix) throws IOException {
        final File instrDir = File.createTempFile(prefix, suffix);
        instrDir.delete();
        instrDir.mkdir();
        return instrDir;
    }

    static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }
}
