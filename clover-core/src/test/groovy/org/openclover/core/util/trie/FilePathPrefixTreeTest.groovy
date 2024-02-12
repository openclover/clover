package org.openclover.core.util.trie

import org.junit.Test

import static org.hamcrest.CoreMatchers.anyOf
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.nullValue
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

/**
 * Test for {@link FilePathPrefixTree}
 */
class FilePathPrefixTreeTest {

    /**
     * @see FilePathPrefixTree#add(java.io.File, Object)
     * @see FilePathPrefixTree#find(java.io.File)
     */
    @Test
    void testAddFindOnFilePaths() {
        FilePathPrefixTree<Integer> trie = new FilePathPrefixTree<Integer>()
        trie.add(new File("/abc/def/ghi"), Integer.valueOf(1))
        trie.add(new File("/abc/def/ghi/jkl"), Integer.valueOf(11))
        trie.add(new File("/abc/def/ghi/mno"), Integer.valueOf(12))
        trie.add(new File("/abc/def/ghi/pqr"), Integer.valueOf(13))
        trie.add(new File("/def"), Integer.valueOf(2))
        trie.add(new File("/def/ghi"), Integer.valueOf(21))

        assertEquals(Integer.valueOf(12), trie.find(new File("/abc/def/ghi/mno")).getValue())
        assertEquals(Integer.valueOf(13), trie.find(new File("/abc/def/ghi/pqr")).getValue())
        assertNull(trie.find(new File("/abc/def/ghi/xyz")))
    }

    /**
     * @see FilePathPrefixTree#add(java.io.File, java.lang.Object)
     * @see FilePathPrefixTree#findNearest(java.io.File)
     */
    @Test
    void testAddFindNearestOnFilePaths() {
        FilePathPrefixTree<Integer> trie = new FilePathPrefixTree<Integer>(Integer.valueOf(0))
        trie.add(new File("/abc"), Integer.valueOf(1))
        trie.add(new File("/abc/def"), Integer.valueOf(2))
        trie.add(new File("/abc/def/ghi"), Integer.valueOf(3))
        trie.add(new File("/jkl"), Integer.valueOf(4))
        trie.add(new File("/jkl/mno"), Integer.valueOf(5))

        // find our roots
        assertEquals(Integer.valueOf(1), trie.findNearest(new File("/abc")).getValue())
        assertEquals(Integer.valueOf(2), trie.findNearest(new File("/abc/def")).getValue())
        assertEquals(Integer.valueOf(3), trie.findNearest(new File("/abc/def/ghi")).getValue())
        assertEquals(Integer.valueOf(4), trie.findNearest(new File("/jkl")).getValue())
        assertEquals(Integer.valueOf(5), trie.findNearest(new File("/jkl/mno")).getValue())

        // find something directly under roots
        assertEquals(Integer.valueOf(1), trie.findNearest(new File("/abc/Foo.java")).getValue())
        assertEquals(Integer.valueOf(2), trie.findNearest(new File("/abc/def/Foo.java")).getValue())
        assertEquals(Integer.valueOf(3), trie.findNearest(new File("/abc/def/ghi/Foo.java")).getValue())
        assertEquals(Integer.valueOf(4), trie.findNearest(new File("/jkl/Foo.java")).getValue())
        assertEquals(Integer.valueOf(5), trie.findNearest(new File("/jkl/mno/Foo.java")).getValue())

        // find something few levels deeper
        assertEquals(Integer.valueOf(1), trie.findNearest(new File("/abc/some/more/levels/Foo.java")).getValue())
        assertEquals(Integer.valueOf(3), trie.findNearest(new File("/abc/def/ghi/some/more/levels/Foo.java")).getValue())

        // test "not found"; tricky stuff: the findNearest() will either find a rootNode (on Linux/MacOS) or a first
        // node under a rootNode (on Windows; a reason is that new File("/") is resolved to "C:", which is a drive letter)
        assertThat(
                trie.findNearest(new File("/")).getValue(),
                anyOf(nullValue(), equalTo(Integer.valueOf(0))))
        assertThat(
                trie.findNearest(new File("/something/outside/our/roots/Foo.java")).getValue(),
                anyOf(nullValue(), equalTo(Integer.valueOf(0))))
    }
}
