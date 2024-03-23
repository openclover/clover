package org.openclover.core.registry.format

import org.junit.Test
import org.openclover.core.api.registry.FileInfo
import org.openclover.core.context.ContextStore
import org.openclover.core.registry.ModelBuilder

import java.nio.channels.FileChannel

import static org.junit.Assert.assertEquals

class InstrSessionSegmentTest {

    @Test
    void testRoundTrip() throws Exception {
        final File temp = File.createTempFile("instrsession", "seg")
        temp.deleteOnExit()

        final ModelBuilder modelBuilder = new ModelBuilder()
        modelBuilder
            .proj("My Project")
                .pkg("com.foo.bar")
                    .file("Baz.java").withId("baz")
                        .clazz("Baz")
                        .endInFile()
                    .end()
                    .file("Bar.java").withId("bar")
                        .clazz("Bar")
                        .endInFile()
                    .end()
                    .file("Bing.java").withId("bing")
                        .clazz("Bing")
                        .endInFile()
                    .end()
                .end()
            .end()

        final List<FileInfoRecord> fileInfos = [
                new FileInfoRecord((FileInfo) modelBuilder.get("baz")),
                new FileInfoRecord((FileInfo) modelBuilder.get("bar")),
                new FileInfoRecord((FileInfo) modelBuilder.get("bing"))
        ]

        final FileChannel outChannel = new FileOutputStream(temp).getChannel()
        try {
            final long now = System.currentTimeMillis()
            new InstrSessionSegment(99l, now, now + 1, fileInfos, new ContextStore()).write(outChannel)
            outChannel.force(true)
        } finally {
            outChannel.close()
        }

        final FileChannel inChannel = new FileInputStream(temp).getChannel()
        try {
            //Start at the last byte
            inChannel.position(inChannel.size() - 1)
            final InstrSessionSegment segment = new InstrSessionSegment(inChannel)

            assertEquals(99L, segment.getVersion())
            assertEquals(3, segment.getFileInfoRecords().size())
        } finally {
            inChannel.close()
        }
    }
}
