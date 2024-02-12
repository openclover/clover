package org.openclover.core.registry.format

import org.junit.Test
import org.openclover.core.registry.ModelBuilder
import org.openclover.core.registry.entities.FullFileInfo

import java.nio.channels.FileChannel

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull

class FileInfoRecordTest {

    @Test
    void testRoundTrip() throws Exception {
        final File temp = File.createTempFile("fileinfo", "rec")
        temp.deleteOnExit()

        final ModelBuilder modelBuilder = new ModelBuilder()
        final FullFileInfo fileInfo = (FullFileInfo)modelBuilder
                    .proj("My Project")
                        .pkg("com.foo.bar")
                            .file("Baz.java").withId("myfile")
                                .clazz("Baz")
                                .endInFile()
                            .end()
                        .end()
                    .end().get("myfile")

        final FileChannel channel = new FileOutputStream(temp).getChannel()
        try {
            new FileInfoRecord(fileInfo).write(channel)
            channel.force(true)
        } finally {
            channel.close()
        }


        final FileInfoRecord record = new FileInfoRecord(new FileInputStream(temp).getChannel())
        assertEquals("Baz.java", record.getName())
        assertEquals("com.foo.bar", record.getPackageName())
        assertNotNull(record.getFileInfo())
        assertEquals(1, record.getFileInfo().getClasses().size())
        assertNull(record.getFileInfo().getContainingPackage())
    }
}
