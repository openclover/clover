package org.openclover.core.cfg

import org.junit.Test

import static org.junit.Assert.assertEquals

class StorageSizeTest {

    @Test
    void testSimpleCases() {
        assertEquals(StorageSize.fromString("1").getSizeInBytes(), 1)
        assertEquals(StorageSize.fromString("1b").getSizeInBytes(), 1)
        assertEquals(StorageSize.fromString("10").getSizeInBytes(), 10)
        assertEquals(StorageSize.fromString("10b").getSizeInBytes(), 10)
        assertEquals(StorageSize.fromString("2k").getSizeInBytes(), 2000)
        assertEquals(StorageSize.fromString("2K").getSizeInBytes(), 2000)
        assertEquals(StorageSize.fromString("33m").getSizeInBytes(), 33000000)
        assertEquals(StorageSize.fromString("33M").getSizeInBytes(), 33000000)
        assertEquals(StorageSize.fromString("45g").getSizeInBytes(), 45000000000L)
        assertEquals(StorageSize.fromString("45G").getSizeInBytes(), 45000000000L)
    }

}
