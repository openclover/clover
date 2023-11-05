package com.atlassian.clover.registry.format;

import com.atlassian.clover.registry.CorruptedRegistryException;
import com.atlassian.clover.registry.IncompatibleRegistryFormatException;
import com.atlassian.clover.registry.RegistryFormatException;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.zip.Adler32;

/**
 * Encapsulates the header of a Clover registry file. Great care has been taken
 * to ensure this class can be used with NIO and with old IO. Old IO usage
 * is necessary so that Java 1.4 is not required at runtime.
 */
public class RegHeader {
    public static final long REG_MAGIC = 0xCAFEFEED;
    public static final int REG_FORMAT_VERSION = 40502; /* A.BB.CC 4.5.2 update this value whenever format is changed */
    public static final int MAX_NAME_LENGTH = 64;
    /** Size in bytes of the header */
    public static final int SIZE = 52 + (MAX_NAME_LENGTH * 4);

    /** Mode: readonly / readwrite */
    private final RegAccessMode accessMode;
    /** Registry version */
    private final long version;
    /** The number of slots in the reg. Needed int the header to allow access to it at runtime */
    private final int slotCount;
    /** Where coverage data starts in the file */
    private final long coverageLocation;
    /** Where the most recent instr session starts in the file. */
    private final long lastSessionLocation;
    /** Name of the registry */
    private final String name;

    RegHeader(RegAccessMode accessMode, long version, int slotCount, long coverageLocation, long lastSessionLocation, String name) {
        this.accessMode = accessMode;
        this.version = version;
        this.slotCount = slotCount;
        this.coverageLocation = coverageLocation;
        this.lastSessionLocation = lastSessionLocation;
        this.name = name;
    }

    ///CLOVER:OFF
    public RegAccessMode getAccessMode() {
        return accessMode;
    }

    public int getSlotCount() {
        return slotCount;
    }

    public long getVersion() {
        return version;
    }

    public long getLastSessionLocation() {
        return lastSessionLocation;
    }

    public long getCoverageLocation() {
        return coverageLocation;
    }

    public String getName() {
        return name;
    }
    ///CLOVER:ON

    /**
     * Read the registry header.
     * Used at recording time by recorders to find the size of the data array
     * @param registryFile the registry file
     * @return the header of the file
     * @throws IOException if an unexpected IO problem occurs while reading
     * @throws RegistryFormatException if a known registry problem is encountered
     */
    public static RegHeader readFrom(File registryFile) throws IOException, RegistryFormatException {
        try (DataInputStream stream = new DataInputStream(Files.newInputStream(registryFile.toPath()))) {
            return readFrom(new StreamInputSource(registryFile.getAbsolutePath(), stream));
        } catch (EOFException e) {
            throw new CorruptedRegistryException(
                    "The Clover registry file \"" + registryFile.getAbsolutePath() + "\" is invalid (truncated header). Please regenerate.");
        }
    }

    /**
     * Read the registry header via a NIO FileChannel.
     * @param name the name of the registry file
     * @param channel the file channel from which to read
     * @return the header of the file
     * @throws IOException if an unexpected IO problem occurs while reading
     * @throws RegistryFormatException if a known registry problem is encountered
     */
    public static RegHeader readFrom(String name, FileChannel channel) throws IOException, RegistryFormatException {
        try {
            return readFrom(new BufferInputSource(name, BufferUtils.readFully(channel, ByteBuffer.allocate(SIZE))));
        } catch (BufferUnderflowException e) {
            throw new CorruptedRegistryException(
                "The Clover registry file \"" + name + "\" is invalid (truncated header). Please regenerate.");
        }
    }

    /**
     * Read header using an abstraction on the underlying data source. Necessary to avoid NIO references
     * in a 1.3 or lesser environment.
     */
    protected static RegHeader readFrom(HeaderInputSource dis) throws IOException, RegistryFormatException {
        final long magic = dis.getLong();
        if (REG_MAGIC != magic) {
            throw new CorruptedRegistryException(
                "File \"" + dis.getName() + "\" is not a valid Clover registry file (file magic number invalid - expected 0x" +
                Integer.toHexString((int)REG_MAGIC) + " but was 0x" + Integer.toHexString((int)magic) + "). Please regenerate.");
        }
        final int regFormat = dis.getInt();
        if (REG_FORMAT_VERSION != regFormat) {
            throw new IncompatibleRegistryFormatException(
                "Clover is no longer compatible with the registry file \"" + dis.getName() +
                "\" (format version " + regFormat + ", supported " + REG_FORMAT_VERSION + "). Please regenerate.");
        }

        final Adler32 checksum = new Adler32();
        final int mode = dis.getInt(checksum);
        final long version = dis.getLong(checksum);
        final int slotCount = dis.getInt(checksum);
        final long coverageLocation = dis.getLong(checksum);
        final long lastSessionLocation = dis.getLong(checksum);

        final char[] name = new char[MAX_NAME_LENGTH];
        for (int i = 0; i < name.length; i++) {
            name[i] = dis.getChar(checksum);
        }

        if (dis.getLong() != checksum.getValue()) {
            throw new CorruptedRegistryException(
                "Clover registry File \"" + dis.getName() + "\" may have been corrupted (header checksum incorrect). Please regenerate.");
        }

        return new RegHeader(RegAccessMode.getFor(mode), version, slotCount, coverageLocation, lastSessionLocation, new String(name).trim());
    }

    public void write(FileChannel channel) throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate(SIZE);
        buffer.putLong(REG_MAGIC);                                             //8 bytes +
        buffer.putInt(REG_FORMAT_VERSION);                                     //4 bytes +

        Adler32 checksum = new Adler32();
        BufferUtils.putWithChecksum(buffer, accessMode.getValue(), checksum);  //4 bytes +
        BufferUtils.putWithChecksum(buffer, version, checksum);                //8 bytes +
        BufferUtils.putWithChecksum(buffer, slotCount, checksum);              //4 bytes +
        BufferUtils.putWithChecksum(buffer, coverageLocation, checksum);       //8 bytes +
        BufferUtils.putWithChecksum(buffer, lastSessionLocation, checksum);    //8 bytes +
                                                                               //---------
                                                                               //44 bytes +
        //Pad to 64 chars... or...                                             //128 bytes +
        final char[] chars = String.format("%-" + MAX_NAME_LENGTH + "." + MAX_NAME_LENGTH + "s", name).toCharArray();
        for (char oneChar : chars) {
            BufferUtils.putWithChecksum(buffer, oneChar, checksum);
        }

        buffer.putLong(checksum.getValue());                                   //8 bytes
                                                                               //---------
                                                                               //=180 bytes

        buffer.flip();

        BufferUtils.writeFully(channel, buffer);
    }

    /**
     * Abstraction to avoid NIO specific code when instrumented applications are running
     * yet allow shared behaviour across NIO and IO.
     **/
    private interface HeaderInputSource {
        String getName();
        long getLong() throws IOException;
        int getInt() throws IOException;
        long getLong(Adler32 checksum) throws IOException;
        int getInt(Adler32 checksum) throws IOException;
        char getChar(Adler32 checksum) throws IOException;
    }

    /** {@link HeaderInputSource} from a {@link ByteBuffer} */
    private static class BufferInputSource implements HeaderInputSource {
        private final String name;
        private final ByteBuffer buffer;

        private BufferInputSource(String name, ByteBuffer buffer) {
            this.name = name;
            this.buffer = buffer;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getLong() {
            return buffer.getLong();
        }

        @Override
        public int getInt() {
            return buffer.getInt();
        }

        @Override
        public long getLong(Adler32 checksum) {
            return BufferUtils.getLongWithChecksum(buffer, checksum);
        }

        @Override
        public int getInt(Adler32 checksum) {
            return BufferUtils.getIntWithChecksum(buffer, checksum);
        }

        @Override
        public char getChar(Adler32 checksum) {
            //Chars are stored/retrieved as ints because DataInputStream and (by default)
            //ByteBuffers disagree on representation of chars
            return (char)BufferUtils.getIntWithChecksum(buffer, checksum);
        }
    }

    /** {@link HeaderInputSource} from a {@link DataInputStream} */
    private static class StreamInputSource implements HeaderInputSource {
        private final DataInputStream dis;
        private final String name;

        private StreamInputSource(String name, DataInputStream dis) {
            this.name = name;
            this.dis = dis;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getLong() throws IOException {
            return dis.readLong();
        }

        @Override
        public int getInt() throws IOException {
            return dis.readInt();
        }

        @Override
        public long getLong(Adler32 checksum) throws IOException {
            final long result = dis.readLong();
            checksum.update((int)(result & 0xFFFFL));
            checksum.update((int)(result >> 32));
            return result;
        }

        @Override
        public int getInt(Adler32 checksum) throws IOException {
            final int result = dis.readInt();
            checksum.update(result);
            return result;
        }

        @Override
        public char getChar(Adler32 checksum) throws IOException {
            //Chars are stored/retrieved as ints because DataInputStream and ByteBuffers disagree
            //on representation of chars
            return (char)getInt(checksum);
        }
    }
}
