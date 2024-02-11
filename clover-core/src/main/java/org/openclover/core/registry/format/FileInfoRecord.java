package org.openclover.core.registry.format;

import org.openclover.core.io.tags.TaggedIO;
import org.openclover.core.registry.entities.FullFileInfo;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

public class FileInfoRecord {
    private static final int MARKER = 0xedd1e;

    private final String name;
    private final String packageName;
    private final LazyProxy<FullFileInfo> fileInfo;

    public FileInfoRecord(FullFileInfo fileInfo) {
        this.name = fileInfo.getName();
        this.packageName = fileInfo.getContainingPackage().getName();
        this.fileInfo = new LazyProxy.Preloaded<>(fileInfo);
    }

    public FileInfoRecord(FileChannel channel) throws IOException {
        //Don't buffer - we need accurate channel positions here and buffering reads eagerly; besides - it's a small amt of data
        final DataInputStream in = new DataInputStream(Channels.newInputStream(channel));

        int mayBeMarker = in.readInt();
        if (mayBeMarker != MARKER) {
            throw new IOException("FileInfoRecord did not start with marker 0x" + Integer.toHexString(MARKER) + ", started with 0x" + Integer.toHexString(mayBeMarker) + " instead");
        }
        name = in.readUTF();
        packageName = in.readUTF();
        final long endPos = (long)in.readInt() + channel.position();

        fileInfo = new LazyLoader<FullFileInfo>(channel, channel.position()) {
            @Override
            protected FullFileInfo getImpl(FileChannel channel) throws IOException {
                return TaggedIO.read(channel, InstrSessionSegment.TAGS, FullFileInfo.class);
            }
        };

        channel.position(endPos);
    }

    public String getName() {
        return name == null ? getFileInfo().getName() : name;
    }

    public String getPackageName() {
        return packageName == null ? getFileInfo().getContainingPackage().getName() : packageName;
    }

    public FullFileInfo getFileInfo() {
        return fileInfo.get();
    }

    public void write(FileChannel channel) throws IOException {
        final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Channels.newOutputStream(channel)));
        out.writeInt(MARKER);
        out.writeUTF(getName());
        out.writeUTF(getPackageName());
        out.flush();

        final long lengthPos = channel.position();
        out.writeInt(-1);
        out.flush();

        TaggedIO.write(channel, InstrSessionSegment.TAGS, FullFileInfo.class, fileInfo.get());
        final long endPos = channel.position();
        final long fileInfoSize = endPos - lengthPos - 4;

        channel.position(lengthPos);
        out.writeInt((int)fileInfoSize);
        out.flush();

        //Make sure we end... at the end!
        channel.position(endPos);
    }
}
