package com.atlassian.clover.io.tags;

import org.openclover.runtime.CloverNames;
import org.openclover.runtime.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class TaggedIO {
    private static final boolean DEBUG;

    ///CLOVER:OFF
    static {
        boolean shouldDebug = false;
        try {
            shouldDebug = Logger.isDebug() && AccessController.doPrivileged((PrivilegedAction<Boolean>) () ->
                    Boolean.getBoolean(CloverNames.PROP_LOGGING_TAGGED_IO));
        } catch (Exception e) {
            Logger.getInstance().warn("Unable to query if tagged IO should be logged", e);
        }
        DEBUG = shouldDebug;
    }
    ///CLOVER:ON

    public static boolean isDebug() {
        return DEBUG;
    }

    public static <T extends TaggedPersistent> T read(FileChannel channel, Tags tags, Class<T> clazz) throws IOException {
        return new TaggedInputReader(new DataInputStream(new BufferedInputStream(Channels.newInputStream(channel))), tags).read(clazz);
    }

    public static <T extends TaggedPersistent> T read(DataInput in, Tags tags, Class<T> clazz) throws IOException {
        return new TaggedInputReader(in, tags).read(clazz);
    }

    public static <T extends TaggedPersistent> void write(FileChannel channel, Tags tags, Class<T> clazz, T object) throws IOException {
        final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Channels.newOutputStream(channel)));
        new TaggedOutputWriter(out, tags).write(clazz, object);
        out.flush();
    }

}