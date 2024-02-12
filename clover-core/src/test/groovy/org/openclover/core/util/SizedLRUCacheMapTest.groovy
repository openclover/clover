package org.openclover.core.util

import org.junit.Test
import org.openclover.runtime.util.ByteSized

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class SizedLRUCacheMapTest {
    static class CacheObject implements ByteSized {
        private final int size

        CacheObject(int size) {
            this.size = size
        }

        long sizeInBytes() {
            return size
        }
    }

    @Test
    void testLengthAfterAdding() {
        SizedLRUCacheMap<String,CacheObject> map = new SizedLRUCacheMap<String, CacheObject>(1000, 10, 1.1f)
        map.put("object1", new CacheObject(500))
        map.put("object2", new CacheObject(500))

        assertEquals(1000, map.getCurrentSizeInBytes())
    }

    @Test
    void testLengthAfterReplacing() {
        SizedLRUCacheMap<String,CacheObject> map = new SizedLRUCacheMap<String, CacheObject>(1000, 10, 1.1f)
        map.put("object1", new CacheObject(500))
        map.put("object2", new CacheObject(500))

        assertEquals(1000, map.getCurrentSizeInBytes())

        map.put("object2", new CacheObject(400))

        assertEquals(900, map.getCurrentSizeInBytes())
    }

    @Test
    void testByteSizeAfterClearing() {
        SizedLRUCacheMap<String,CacheObject> map = new SizedLRUCacheMap<String, CacheObject>(1000, 10, 1.1f)
        map.put("object1", new CacheObject(500))
        map.put("object2", new CacheObject(500))

        assertEquals(1000, map.getCurrentSizeInBytes())

        map.clear()
        assertEquals(0, map.getCurrentSizeInBytes())
    }

    @Test
    void testByteSizeAfterRemoving() {
        SizedLRUCacheMap<String,CacheObject> map = new SizedLRUCacheMap<String, CacheObject>(1000, 10, 1.1f)
        map.put("object1", new CacheObject(500))
        map.put("object2", new CacheObject(500))

        assertEquals(1000, map.getCurrentSizeInBytes())

        map.remove("object1")
        assertEquals(500, map.getCurrentSizeInBytes())

        //Not found
        map.remove("foo")
        assertEquals(500, map.getCurrentSizeInBytes())
    }

    @Test
    void testOutgrowthEvictsLRU() {
        Map<String,CacheObject> map = new SizedLRUCacheMap<String, CacheObject>(1000, 10, 1.1f)
        map.put("object1", new CacheObject(500))
        map.put("object2", new CacheObject(500))
        //One object too many, object1 should be evicted
        map.put("object3", new CacheObject(500))
        assertFalse(map.containsKey("object1"))
        assertTrue(map.containsKey("object2"))
        assertTrue(map.containsKey("object3"))

        //Make object2 MRU
        map.get("object2")
        //Now a new object should evict object3, not object2
        map.put("object4", new CacheObject(500))

        assertFalse(map.containsKey("object3"))
        assertTrue(map.containsKey("object2"))
        assertTrue(map.containsKey("object4"))
    }

}
