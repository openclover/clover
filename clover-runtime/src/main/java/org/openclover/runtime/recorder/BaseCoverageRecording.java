package org.openclover.runtime.recorder;

import org.openclover.runtime.registry.format.RegHeader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * {@link BaseCoverageRecording} is a {@link CoverageRecording} with a {@link Header}
 */
public abstract class BaseCoverageRecording implements CoverageRecording {
    protected final Header header;
    protected final File fileOnDisk;

    public BaseCoverageRecording(Header header, File fileOnDisk) {
        this.header = header;
        this.fileOnDisk = fileOnDisk;
    }

    @Override
    public long getDbVersion() {
        return header.dbVersion;
    }

    public long getWriteTimeStamp() {
        return header.writeTimeStamp;
    }

    @Override
    public int getFormat() {
        return header.format;
    }

    protected File createCoverageFolderFor(File file) throws IOException {
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new IOException("Failed to create parent folders for coverage file " + file.getAbsolutePath());
            }
        }
        return file;
    }

    public File getFile() {
        return fileOnDisk;
    }

    /**
     * {@link Header} - the header for {@link CoverageRecording} files
     */
    public static class Header {
        public static final long REC_MAGIC = 0xb41b41;
        public static final int REC_FORMAT_VERSION = RegHeader.REG_FORMAT_VERSION;
        protected long dbVersion;
        protected long writeTimeStamp;
        protected int format;

        public Header(DataInputStream in) throws IOException {
            read(in);
        }

        public Header(long dbVersion, long writeTimeStamp, int format) {
            this.dbVersion = dbVersion;
            this.writeTimeStamp = writeTimeStamp;
            this.format = format;
        }

        protected void read(DataInputStream in) throws IOException {
            final long magic = in.readLong();
            if (magic != REC_MAGIC) {
                throw new IOException("This is not a valid OpenClover recording file or was generated by a previous version of OpenClover.");
            }
            final int registryFormat = in.readInt();
            if (registryFormat != REC_FORMAT_VERSION) {
                throw new IOException("This recording file was generated by a "
                        + ((registryFormat < REC_FORMAT_VERSION) ? "previous" : "subsequent") + " version of OpenClover.");
            }
            dbVersion = in.readLong();
            writeTimeStamp = in.readLong();
            format = in.read();
        }

        protected void write(DataOutputStream out) throws IOException {
            out.writeLong(REC_MAGIC);
            out.writeInt(REC_FORMAT_VERSION);
            out.writeLong(dbVersion);
            out.writeLong(writeTimeStamp);
            out.write(format);
        }

        public long getWriteTimeStamp() {
            return writeTimeStamp;
        }

        public long getDbVersion() {
            return dbVersion;
        }

        public int getFormat() {
            return format;
        }

        public String toString() {
            return
                "Header[dbVersion=" + dbVersion +
                ", writeTimeStamp=" + writeTimeStamp +
                ", format=" + format + "]";
        }
    }
}
