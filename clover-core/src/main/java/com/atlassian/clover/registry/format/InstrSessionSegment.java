package com.atlassian.clover.registry.format;

import com.atlassian.clover.io.tags.ObjectReader;
import com.atlassian.clover.io.tags.TaggedDataInput;
import com.atlassian.clover.io.tags.TaggedIO;
import com.atlassian.clover.io.tags.Tags;
import com.atlassian.clover.Logger;
import com.atlassian.clover.context.ContextSet;
import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.context.MethodRegexpContext;
import com.atlassian.clover.context.StatementRegexpContext;
import com.atlassian.clover.registry.entities.AnnotationImpl;
import com.atlassian.clover.registry.entities.ArrayAnnotationValue;
import com.atlassian.clover.registry.entities.FullBranchInfo;
import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.FixedSourceRegion;
import com.atlassian.clover.registry.entities.FullMethodInfo;
import com.atlassian.clover.registry.entities.MethodSignature;
import com.atlassian.clover.registry.entities.Modifiers;
import com.atlassian.clover.registry.entities.Parameter;
import com.atlassian.clover.registry.entities.FullStatementInfo;
import com.atlassian.clover.registry.entities.StringifiedAnnotationValue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.openclover.util.Lists.newLinkedList;

public class InstrSessionSegment {
    static final long NONE_IDX = -1L;
    static final Tags TAGS =
        new Tags()
                .registerTag(FullFileInfo.class.getName(), Tags.NEXT_TAG + 0, (ObjectReader<FullFileInfo>) FullFileInfo::read)
                .registerTag(FullClassInfo.class.getName(), Tags.NEXT_TAG + 1, (ObjectReader<FullClassInfo>) FullClassInfo::read)
                .registerTag(FullMethodInfo.class.getName(), Tags.NEXT_TAG + 2, (ObjectReader<FullMethodInfo>) FullMethodInfo::read)
                .registerTag(FullStatementInfo.class.getName(), Tags.NEXT_TAG + 3, (ObjectReader<FullStatementInfo>) FullStatementInfo::read)
                .registerTag(FullBranchInfo.class.getName(), Tags.NEXT_TAG + 4, (ObjectReader<FullBranchInfo>) FullBranchInfo::read)
                .registerTag(ContextSet.class.getName(), Tags.NEXT_TAG + 5, (ObjectReader<ContextSet>) ContextSet::read)
                .registerTag(FixedSourceRegion.class.getName(), Tags.NEXT_TAG + 6, (ObjectReader<FixedSourceRegion>) FixedSourceRegion::read)
                .registerTag(MethodSignature.class.getName(), Tags.NEXT_TAG + 7, (ObjectReader<MethodSignature>) MethodSignature::read)
                .registerTag(Modifiers.class.getName(), Tags.NEXT_TAG + 8, (ObjectReader<Modifiers>) Modifiers::read)
                .registerTag(Parameter.class.getName(), Tags.NEXT_TAG + 9, (ObjectReader<Parameter>) Parameter::read)
                .registerTag(AnnotationImpl.class.getName(), Tags.NEXT_TAG + 10, (ObjectReader<AnnotationImpl>) AnnotationImpl::read)
                .registerTag(ArrayAnnotationValue.class.getName(), Tags.NEXT_TAG + 11, (ObjectReader<ArrayAnnotationValue>) ArrayAnnotationValue::read)
                .registerTag(StringifiedAnnotationValue.class.getName(), Tags.NEXT_TAG + 12, (ObjectReader<StringifiedAnnotationValue>) StringifiedAnnotationValue::read)
                .registerTag(ContextStore.class.getName(), (byte)(Tags.NEXT_TAG + 13), (ObjectReader<ContextStore>) ContextStore::read)
                .registerTag(StatementRegexpContext.class.getName(), (byte)(Tags.NEXT_TAG + 14), (ObjectReader<StatementRegexpContext>) StatementRegexpContext::read)
                .registerTag(MethodRegexpContext.class.getName(), (byte)(Tags.NEXT_TAG + 15), (ObjectReader<MethodRegexpContext>) MethodRegexpContext::read);

    private final long version;
    private final long startTs;
    private final long endTs;
    private final List<FileInfoRecord> fileInfoRecords;
    private final LazyProxy<ContextStore> ctxStore;

    private static class Footer {
        public static final int SIZE = 40;
        public static final int MARKER = 0xdada;

        public final int fileInfosByteLen;
        public final int ctxStoreByteLen;
        public final int numRecords;
        public final long endTs;
        public final long startTs;
        public final long version;

        private Footer(int fileInfosByteLen, int ctxStoreByteLen, int numRecords, long endTs, long startTs, long version) {
            this.fileInfosByteLen = fileInfosByteLen;
            this.ctxStoreByteLen = ctxStoreByteLen;
            this.numRecords = numRecords;
            this.endTs = endTs;
            this.startTs = startTs;
            this.version = version;
        }

        @Override
        public String toString() {
            return "Footer{" +
                "fileInfosByteLen=" + fileInfosByteLen +
                ", ctxStoreByteLen=" + ctxStoreByteLen +
                ", numRecords=" + numRecords +
                ", endTs=" + endTs +
                ", startTs=" + startTs +
                ", version=" + version +
                '}';
        }
    }

    public InstrSessionSegment(FileChannel channel) throws IOException {
        final long endOfSegment = channel.position();

        final Footer footer = loadFooter(channel, endOfSegment);
        this.version = footer.version;
        this.startTs = footer.startTs;
        this.endTs = footer.endTs;

        this.ctxStore = new LazyLoader<ContextStore>(channel, endOfSegment - Footer.SIZE - footer.ctxStoreByteLen + 1) {
            @Override
            protected ContextStore getImpl(FileChannel channel) throws IOException {
                return loadContextStore(channel);
            }
        };
        this.fileInfoRecords = loadFileInfos(channel, endOfSegment, footer.fileInfosByteLen, footer.ctxStoreByteLen, footer.numRecords);

        //Position channel at very start of record ready to rewind to previous session segment
        channel.position(Math.max(0, endOfSegment - Footer.SIZE - footer.fileInfosByteLen - footer.ctxStoreByteLen));
    }

    public InstrSessionSegment(long version, long startTs, long endTs, List<FileInfoRecord> fileInfoRecords, ContextStore ctxStore) {
        this.version = version;
        this.startTs = startTs;
        this.endTs = endTs;
        this.fileInfoRecords = Collections.unmodifiableList(newLinkedList(fileInfoRecords));
        this.ctxStore = new LazyProxy.Preloaded<>(ctxStore);
    }

    private Footer loadFooter(FileChannel channel, long endOfSegment) throws IOException {
        //Position channel at start of header
        channel.position(endOfSegment - Footer.SIZE + 1);

        final ByteBuffer footerBuffer = BufferUtils.readFully(channel, ByteBuffer.allocate(Footer.SIZE));

        final Footer footer =
            new Footer(
                footerBuffer.getInt(),
                footerBuffer.getInt(),
                footerBuffer.getInt(),
                footerBuffer.getLong(),
                footerBuffer.getLong(),
                footerBuffer.getLong());
        Logger.getInstance().debug("Loaded InstrumentationSession footer: " + footer);
        if (footerBuffer.getInt() != Footer.MARKER) {
            throw new IOException("InstrSessionSegment did not start with marker 0x" + Integer.toHexString(Footer.MARKER));
        }
        return footer;
    }

    private List<FileInfoRecord> loadFileInfos(FileChannel channel, long endOfSegment, int fileInfosByteLen, int offset, int numRecords) throws IOException {
        //Position channel at very start of record to read FullFileInfo bytes
        final long startRecPos = endOfSegment - Footer.SIZE - offset - fileInfosByteLen + 1;
        Logger.getInstance().debug("Loading InstrumentationSession: Loading FileInfos: setting channel position to " + startRecPos + " out of " + channel.size());
        channel.position(startRecPos);

        final List<FileInfoRecord> fileInfos = newLinkedList();
        for(int i = 0; i < numRecords; i++) {
            fileInfos.add(new FileInfoRecord(channel));
        }
        return Collections.unmodifiableList(fileInfos);
    }

    private ContextStore loadContextStore(FileChannel channel) throws IOException {
        return TaggedIO.read(channel, TAGS, ContextStore.class);
    }

    public Collection<FileInfoRecord> getFileInfoRecords() {
        return fileInfoRecords;
    }

    public long getVersion() {
        return version;
    }

    public long getStartTs() {
        return startTs;
    }

    public long getEndTs() {
        return endTs;
    }

    public ContextStore getCtxStore() {
        return ctxStore.get();
    }

    public void write(FileChannel channel) throws IOException {
        final long startPos = channel.position();

        for (FileInfoRecord fileInfo : fileInfoRecords) {
            fileInfo.write(channel);
        }
        final long afterFileInfosPos = channel.position();

        TaggedIO.write(channel, TAGS, ContextStore.class, ctxStore.get());
        final long afterCtxStorePos = channel.position();

        final ByteBuffer headerBuffer = ByteBuffer.allocate(Footer.SIZE);
        headerBuffer.putInt((int)(afterFileInfosPos - startPos));           //4
        headerBuffer.putInt((int)(afterCtxStorePos - afterFileInfosPos));   //4
        headerBuffer.putInt(fileInfoRecords.size());                        //4
        headerBuffer.putLong(endTs);                                        //8
        headerBuffer.putLong(startTs);                                      //8
        headerBuffer.putLong(version);                                      //8
        headerBuffer.putInt(Footer.MARKER);                                 //4
        headerBuffer.flip();                                                //=40
        BufferUtils.writeFully(channel, headerBuffer);
    }
}
