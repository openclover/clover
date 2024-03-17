package org.openclover.core.registry.format

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.openclover.runtime.registry.CorruptedRegistryException
import org.openclover.runtime.registry.IncompatibleRegistryFormatException
import org.openclover.runtime.registry.format.BufferUtils
import org.openclover.runtime.registry.format.RegAccessMode
import org.openclover.runtime.registry.format.RegHeader

import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertSame
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class RegHeaderTest {

    @Rule
    public TestName testName = new TestName()

    @Test
    void testTruncation() throws Exception {
        final File regFile = File.createTempFile("clover", "db")
        regFile.deleteOnExit()
        final FileChannel channel = new FileOutputStream(regFile).getChannel()
        try {
            BufferUtils.writeFully(
                channel,
                (ByteBuffer)ByteBuffer.allocate(12)
                    .putLong(RegHeader.REG_MAGIC)
                    .putInt(RegHeader.REG_FORMAT_VERSION)
                    .flip())
        } finally {
            channel.close()
        }

        //Read using FileChannel
        try {
            final FileChannel inChannel = new FileInputStream(regFile).getChannel()
            RegHeader.readFrom(regFile.getAbsolutePath(), inChannel)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (CorruptedRegistryException e) {
            assertTrue(e.getMessage().contains("(truncated header)"))
        }

        //Read using DataInputStream
        try {
            RegHeader.readFrom(regFile)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (CorruptedRegistryException e) {
            assertTrue(e.getMessage().contains("(truncated header)"))
        }
    }

    @Test
    void testInvalidMagicNumber() throws Exception {
        final File regFile = File.createTempFile("clover", "db")
        regFile.deleteOnExit()
        final FileChannel channel = new FileOutputStream(regFile).getChannel()
        try {
            new RegHeader(RegAccessMode.READWRITE, 0L, 0, CoverageSegment.NONE_IDX, InstrSessionSegment.NONE_IDX,
                    testName.methodName).write(channel)
            channel.position(0)
            BufferUtils.writeFully(channel, (ByteBuffer)ByteBuffer.allocate(8).putLong(RegHeader.REG_MAGIC + 1).flip())
        } finally {
            channel.close()
        }

        //Read using FileChannel
        try {
            final FileChannel inChannel = new FileInputStream(regFile).getChannel()
            RegHeader.readFrom(regFile.getAbsolutePath(), inChannel)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (CorruptedRegistryException e) {
            assertTrue(e.getMessage().contains("file magic number invalid"))
        }

        //Read using DataInputStream
        try {
            RegHeader.readFrom(regFile)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (CorruptedRegistryException e) {
            assertTrue(e.getMessage().contains("file magic number invalid"))
        }
    }

    @Test
    void testIncompatibleFormatVersion() throws Exception {
        final File regFile = File.createTempFile("clover", "db")
        regFile.deleteOnExit()
        final FileChannel channel = new FileOutputStream(regFile).getChannel()
        try {
            new RegHeader(RegAccessMode.READWRITE, 0L, 0, CoverageSegment.NONE_IDX, InstrSessionSegment.NONE_IDX,
                    testName.methodName).write(channel)
            channel.position(0)
            BufferUtils.writeFully(
                channel,
                (ByteBuffer)ByteBuffer.allocate(12)
                    .putLong(RegHeader.REG_MAGIC)
                    .putInt(RegHeader.REG_FORMAT_VERSION + 1)
                    .flip())
        } finally {
            channel.close()
        }

        //Read using FileChannel
        try {
            final FileChannel inChannel = new FileInputStream(regFile).getChannel()
            RegHeader.readFrom(regFile.getAbsolutePath(), inChannel)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (IncompatibleRegistryFormatException e) {
            assertTrue(e.getMessage().contains("OpenClover is no longer compatible with the registry file"))
        }

        //Read using DataInputStream
        try {
            RegHeader.readFrom(regFile)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (IncompatibleRegistryFormatException e) {
            assertTrue(e.getMessage().contains("OpenClover is no longer compatible with the registry file"))
        }
    }

    @Test
    void testInvalidChecksum() throws Exception {
        final File regFile = File.createTempFile("clover", "db")
        regFile.deleteOnExit()
        FileChannel channel = new FileOutputStream(regFile).getChannel()
        try {
            new RegHeader(RegAccessMode.READWRITE, 0L, 0, CoverageSegment.NONE_IDX, InstrSessionSegment.NONE_IDX,
                    testName.methodName).write(channel)
        } finally {
            channel.force(true)
            channel.close()
        }

        channel = new RandomAccessFile(regFile, "rw").getChannel()
        //Position at the checksum (last 8 bytes of the header)
        try {
            channel.position(RegHeader.SIZE - 8)
            final ByteBuffer buffer = (ByteBuffer)BufferUtils.readFully(channel, ByteBuffer.allocate(8))
            //Add one to the checksum and write it back
            final long badChecksum = buffer.getLong() + 1
            channel.position(RegHeader.SIZE - 8)
            BufferUtils.writeFully(channel, (ByteBuffer)((ByteBuffer)buffer.flip()).putLong(badChecksum).flip())
        } finally {
            channel.force(true)
            channel.close()
        }
         
        //Read using FileChannel
        try {
            final FileChannel inChannel = new FileInputStream(regFile).getChannel()
            RegHeader.readFrom(regFile.getAbsolutePath(), inChannel)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (CorruptedRegistryException e) {
            assertTrue(e.getMessage().contains("header checksum incorrect"))
        }

        //Read using DataInputStream
        try {
            RegHeader.readFrom(regFile)
            ///CLOVER:OFF
            fail()
            ///CLOVER:ON
        } catch (CorruptedRegistryException e) {
            assertTrue(e.getMessage().contains("header checksum incorrect"))
        }
    }

    @Test
    void testRoundtrip() throws Exception {
        final File regFile = File.createTempFile("clover", "db")
        regFile.deleteOnExit()
        FileChannel channel = new FileOutputStream(regFile).getChannel()
        RegHeader header1 = null
        try {
            header1 = new RegHeader(RegAccessMode.READWRITE, 0L, 0, CoverageSegment.NONE_IDX, InstrSessionSegment.NONE_IDX,
                    testName.methodName)
            header1.write(channel)
        } finally {
            channel.close()
        }

        final RegHeader header2 = RegHeader.readFrom(regFile)
        assertEquals(header1.getName(), header2.getName())
        assertSame(header1.getAccessMode(), header2.getAccessMode())
        assertEquals(header1.getCoverageLocation(), header2.getCoverageLocation())
        assertEquals(header1.getSlotCount(), header2.getSlotCount())
        assertEquals(header1.getLastSessionLocation(), header2.getLastSessionLocation())
        assertEquals(header1.getVersion(), header2.getVersion())

        channel = new FileOutputStream(regFile).getChannel()
        RegHeader header3 = null
        try {
            header3 = new RegHeader(RegAccessMode.READONLY, System.currentTimeMillis(), 20, 256L, Long.MAX_VALUE,
                    testName.methodName)
            header3.write(channel)
        } finally {
            channel.close()
        }

        final RegHeader header4 = RegHeader.readFrom(regFile)
        assertEquals(header3.getName(), header4.getName())
        assertSame(header3.getAccessMode(), header4.getAccessMode())
        assertEquals(header3.getCoverageLocation(), header4.getCoverageLocation())
        assertEquals(header3.getSlotCount(), header4.getSlotCount())
        assertEquals(header3.getLastSessionLocation(), header4.getLastSessionLocation())
        assertEquals(header3.getVersion(), header4.getVersion())
    }

    @Test
    void testOversizeNameIsTruncated() throws Exception {
        final File regFile = File.createTempFile("clover", "db")
        regFile.deleteOnExit()

        final StringBuilder name = new StringBuilder(RegHeader.MAX_NAME_LENGTH)
        for(int i = 0; i < RegHeader.MAX_NAME_LENGTH + 1; i++) {
            name.append('0' + i)
        }
        final FileChannel channel = new FileOutputStream(regFile).getChannel()
        try {
            new RegHeader(RegAccessMode.READWRITE, 0L, 0, CoverageSegment.NONE_IDX, InstrSessionSegment.NONE_IDX, name.toString()).write(channel)
        } finally {
            channel.close()
        }

        //Read using FileChannel
        final FileChannel inChannel = new FileInputStream(regFile).getChannel()
        final RegHeader header = RegHeader.readFrom(regFile.getAbsolutePath(), inChannel)
        assertEquals(RegHeader.MAX_NAME_LENGTH, header.getName().length())
        assertEquals(name.toString().substring(0, RegHeader.MAX_NAME_LENGTH), header.getName())
    }
}
