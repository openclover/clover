package org.openclover.runtime.util;

import org.openclover.runtime.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class IOStreamUtils {

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Logger.getInstance().verbose("Failed to close resource: " + closeable, e);
            }
        }
    }

    public static OutputStream createDeflateOutputStream(final File file) throws FileNotFoundException {
        return new BufferedOutputStream(new DeflaterOutputStream(FOSFactory.newFOS(file), new Deflater(Deflater.BEST_SPEED), 8192));
    }

    public static InputStream createInflaterInputStream(final File file) throws IOException {
        return new BufferedInputStream(new InflaterInputStream(new BufferedInputStream(Files.newInputStream(file.toPath()))));
    }

    public static void writeChars(String str, DataOutputStream out) throws IOException {
        str = str == null ? "" : str;
        out.writeInt(str.length());
        out.writeChars(str);
    }

}
