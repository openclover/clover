package com.atlassian.clover.util;

import com.atlassian.clover.CloverNames;
import com.atlassian.clover.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Factory for FileOutputStreams so that we can vary behaviour
 * depending on system properties. Currently, the only system property that is accepted
 * is "clover.synchronous.io=true|false" which will cause FileDescriptor.sync() to be called
 * after a call to FileOutputStream.flush().
 */
public class FOSFactory {
    private static final boolean USE_SYNCHRONOUS_IO;
    public static final Class[] REQUIRED_CLASSES = {
        SyncingFileOutputStream.class
    };

    static {
        Boolean useSyncIO = Boolean.FALSE;
        try {
            useSyncIO =
                AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                    @Override
                    public Boolean run() {
                        return Boolean.getBoolean(CloverNames.PROP_SYNCHRONOUS_IO);
                    }
            });
        } catch (SecurityException e) {
            Logger.getInstance().info("Unable to determine Clover IO mode", e);
        }
        USE_SYNCHRONOUS_IO = useSyncIO != null ? useSyncIO : false;
    }

    public static FileOutputStream newFOS(File file) throws FileNotFoundException {
        if (USE_SYNCHRONOUS_IO) {
            return new SyncingFileOutputStream(file);
        } else {
            return new FileOutputStream(file);
        }
    }

    private static class SyncingFileOutputStream extends FileOutputStream {
        private SyncingFileOutputStream(File name) throws FileNotFoundException {
            super(name);
        }

        @Override
        public void flush() throws IOException {
            super.flush();
            //For OSs that support this feature, force OS buffers to sync with
            //the file so that subsequent reads will see the flushed data
            //Currently, OSX does *not* respect this
            getFD().sync();
        }
    }
}
