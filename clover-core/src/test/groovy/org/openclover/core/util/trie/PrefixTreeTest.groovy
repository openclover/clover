package org.openclover.core.util.trie

import org.openclover.core.util.Lists
import org.jetbrains.annotations.NotNull
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*
import static org.openclover.core.util.Lists.newArrayList

/**
 * Test for {@link PrefixTree}
 */
class PrefixTreeTest {

    /**
     * <pre>
     *  root         (0)
     *     a
     *     + b       (1)
     *       + c
     *         + d   (2)
     *         + e   (3)
     *     + z       (4)
     * </pre>
     */
    PrefixTree<String, Integer> createSampleTree() {
        final PrefixTree<String, Integer> trie = new PrefixTree<String, Integer>(NodeFactoryImpl.TREE_MAP_BACKED, "", Integer.valueOf(0))
        final KeySequence<String> key1 = new KeySequence<String>(newArrayList("a", "b"))
        final KeySequence<String> key2 = new KeySequence<String>(newArrayList("a", "b", "c", "d"))
        final KeySequence<String> key3 = new KeySequence<String>(newArrayList("a", "b", "c", "e"))
        final KeySequence<String> key4 = new KeySequence<String>(newArrayList("a", "z"))

        trie.add(key1, Integer.valueOf(1))
        trie.add(key2, Integer.valueOf(2))
        trie.add(key3, Integer.valueOf(3))
        trie.add(key4, Integer.valueOf(4))

        return trie
    }

    /**
     * Test empty tree behaviour.
     *
     * @see PrefixTree#add(KeySequence, Object)
     * @see PrefixTree#find(KeySequence)
     */
    @Test
    void testAddFindRootNode() {
        // case: empty trie structure, no value attached to the root node, find the root node
        final PrefixTree<String, Integer> trieWithoutRootValue = new PrefixTree<String, Integer>(NodeFactoryImpl.HASH_MAP_BACKED, "", null)
        final KeySequence<String> emptyKey = new KeySequence<String>()
        Node<String, Integer> nodeFound = trieWithoutRootValue.find(emptyKey)

        assertNotNull(nodeFound)
        assertEquals(trieWithoutRootValue.rootNode, nodeFound)
        assertNull(nodeFound.getValue())

        // case: empty trie structure, value attached to the root node, find the root node
        final PrefixTree<String, Integer> trieWithRootValue = new PrefixTree<String, Integer>(NodeFactoryImpl.HASH_MAP_BACKED, "", Integer.valueOf(10))
        nodeFound = trieWithRootValue.find(emptyKey)

        assertNotNull(nodeFound)
        assertEquals(trieWithRootValue.rootNode, nodeFound)
        assertEquals(Integer.valueOf(10), nodeFound.getValue())

        // case: empty trie structure, value attached to the root node, find not existing subnode
        final KeySequence<String> nonExistingKey = new KeySequence<String>(newArrayList("some", "key"))
        assertNull(trieWithRootValue.find(nonExistingKey))
    }

    /**
     * @see PrefixTree#add(KeySequence, Object)
     * @see PrefixTree#find(KeySequence)
     */
    @Test
    void testAddFind() {
        final PrefixTree<String, Integer> trie = new PrefixTree<String, Integer>(NodeFactoryImpl.HASH_MAP_BACKED, "", null)

        // case: add a single key
        final KeySequence<String> key1 = new KeySequence<String>(newArrayList("abc", "def"))
        trie.add(key1, Integer.valueOf(10))
        assertEquals(Integer.valueOf(10), trie.find(key1).getValue())

        // case: update an existing key
        trie.add(key1, Integer.valueOf(20))
        assertEquals(Integer.valueOf(20), trie.find(key1).getValue())

        // case: add more keys, we should have all of them stored
        final KeySequence<String> key2 = new KeySequence<String>(newArrayList("ghi", "jkl", "mno"))
        trie.add(key2, Integer.valueOf(30))
        assertEquals(Integer.valueOf(20), trie.find(key1).getValue())
        assertEquals(Integer.valueOf(30), trie.find(key2).getValue())

        // case: search for non-existing key
        final KeySequence<String> key3 = new KeySequence<String>(newArrayList("xyz"))
        assertNull(trie.find(key3))
    }

    /**
     * @see PrefixTree#add(KeySequence, Object)
     * @see PrefixTree#findNearest(KeySequence)
     */
    @Test
    void testAddFindNearest() {
        PrefixTree<String, Integer> trie = new PrefixTree<String, Integer>(NodeFactoryImpl.HASH_MAP_BACKED, "", Integer.valueOf(0))
        trie.add(new KeySequence<String>(newArrayList("abc")), Integer.valueOf(1))
        trie.add(new KeySequence<String>(newArrayList("abc", "def")), Integer.valueOf(2))
        trie.add(new KeySequence<String>(newArrayList("abc", "def", "ghi")), Integer.valueOf(3))
        trie.add(new KeySequence<String>(newArrayList("jkl")), Integer.valueOf(4))
        trie.add(new KeySequence<String>(newArrayList("jkl", "mno")), Integer.valueOf(5))

        // find our roots
        assertEquals(
                Integer.valueOf(1),
                trie.findNearest(new KeySequence<String>(newArrayList("abc"))).getValue())
        assertEquals(
                Integer.valueOf(2),
                trie.findNearest(new KeySequence<String>(newArrayList("abc", "def"))).getValue())
        assertEquals(
                Integer.valueOf(3),
                trie.findNearest(new KeySequence<String>(newArrayList("abc", "def", "ghi"))).getValue())
        assertEquals(
                Integer.valueOf(4),
                trie.findNearest(new KeySequence<String>(newArrayList("jkl"))).getValue())
        assertEquals(
                Integer.valueOf(5),
                trie.findNearest(new KeySequence<String>(newArrayList("jkl", "mno"))).getValue())

        // find something directly under roots
        assertEquals(
                Integer.valueOf(1),
                trie.findNearest(new KeySequence<String>(newArrayList("abc", "Foo.java"))).getValue())
        assertEquals(
                Integer.valueOf(2),
                trie.findNearest(new KeySequence<String>(newArrayList("abc", "def", "Foo.java"))).getValue())
        assertEquals(
                Integer.valueOf(3),
                trie.findNearest(new KeySequence<String>(newArrayList("abc", "def", "ghi", "Foo.java"))).getValue())
        assertEquals(
                Integer.valueOf(4),
                trie.findNearest(new KeySequence<String>(newArrayList("jkl", "Foo.java"))).getValue())
        assertEquals(
                Integer.valueOf(5),
                trie.findNearest(new KeySequence<String>(newArrayList("jkl", "mno", "Foo.java"))).getValue())

        // find something few levels deeper
        assertEquals(
                Integer.valueOf(1),
                trie.findNearest(new KeySequence<String>(newArrayList("abc", "some", "more", "levels", "Foo.java"))).getValue())
        assertEquals(
                Integer.valueOf(3),
                trie.findNearest(new KeySequence<String>(newArrayList("abc", "def", "ghi", "some", "more", "levels", "Foo.java"))).getValue())

        // test "not found"; tricky stuff: the findNearest() will either find a rootNode (on Linux/MacOS) or a first
        // node under a rootNode (on Windows; a reason is that new File("/") is resolved to "C:", which is a drive letter)
        assertThat(
                trie.findNearest(new KeySequence<String>(Collections.<String>emptyList())).getValue(),
                anyOf(nullValue(), equalTo(Integer.valueOf(0))))
        assertThat(
                trie.findNearest(new KeySequence<String>(newArrayList("something", "outside", "our", "roots", "Foo.java"))).getValue(),
                anyOf(nullValue(), equalTo(Integer.valueOf(0))))

    }

    /**
     * <pre>
     *  root         (0)
     *     a
     *     + b       (1)
     *       + c
     *         + d   (2)
     *         + e   (3)
     *     + z       (4)
     * </pre>
     *
     * @see PrefixTree#add(KeySequence, Object)
     * @see PrefixTree#findNearestWithValue(KeySequence)
     */
    @Test
    void testAddFindNearestWithValue() {
        final PrefixTree<String, Integer> trie = createSampleTree()

        // case: search for a non-exisiting node, but which is located under an existing intermediate node which does
        // not have a value assigned to it
        // expected: take first non-null value from its ancestors
        final Node<String, Integer> nodeABCFoo = trie.findNearestWithValue(new KeySequence<String>(
                newArrayList("a", "b", "c", "Foo.java")))
        assertNotNull(nodeABCFoo)
        assertEquals(Integer.valueOf(1), nodeABCFoo.getValue()); // take from "a/b" node

        // case: search for an existing intermediate node which does not have a value assigned to it
        // expected: take first non-null value from its ancestors
        final Node<String, Integer> nodeABC = trie.findNearestWithValue(new KeySequence<String>(
                newArrayList("a", "b", "c")))
        assertNotNull(nodeABC)
        assertEquals(Integer.valueOf(1), nodeABC.getValue()); // take from "a/b" node

        // case: search for an existing node which is a direct child of the root node and which does not have any
        // value assigned in it
        // expected: take value from a root node (if any is set)
        final Node<String, Integer> nodeA = trie.findNearestWithValue(new KeySequence<String>(
                newArrayList("a")))
        assertNotNull(nodeA)
        assertEquals(Integer.valueOf(0), nodeA.getValue()); // take from rootNode

        // case: search for a non-existing key
        // expected: root node or null if root node does not have any value set
        final Node<String, Integer> nodeNotFound = trie.findNearestWithValue(new KeySequence<String>(
                newArrayList("not", "existing", "path", "at", "all")))
        assertNotNull(nodeNotFound)
        assertEquals(Integer.valueOf(0), nodeNotFound.getValue()); // take from rootNode

        // case: use empty key sequence in search
        // expected: return root node (if has value in it) or null
        final Node<String, Integer> nodeRoot = trie.findNearestWithValue(new KeySequence<String>(
                Lists.<String>newArrayList()))
        assertNotNull(nodeRoot)
        assertEquals(Integer.valueOf(0), nodeRoot.getValue())

        // check tree structure
        final String expectedTree =
                "+ (0)\n" +
                "  +a\n" +
                "    +b (1)\n" +
                "      +c\n" +
                "        +d (2)\n" +
                "        +e (3)\n" +
                "    +z (4)\n"
        assertPrintTreeEquals(expectedTree, trie)
    }

    static <K,V> void assertPrintTreeEquals(String expectedTree, PrefixTree<K,V> actualTree) {
        final String expected = expectedTree.replace("\n", System.getProperty("line.separator"))
        final ByteArrayOutputStream out = new ByteArrayOutputStream()

        actualTree.printTree(new PrintStream(out), actualTree.rootNode)
        assertEquals(expected, out.toString())
    }

    @Test
    void testWalkTree() {
        final PrefixTree<String, Integer> trie = createSampleTree()

        trie.walkTree(trie.rootNode, new NodeVisitor<String, Integer>() {
            @Override
            Node<String, Integer> visit(@NotNull Node<String, Integer> node, int depth) {
                // our root visitor does not change a tree structure, but only multiplies stored values
                Integer value = node.getValue()
                if (value != null) {
                    value *= depth
                    node.setValue(value)
                }
                return node
            }
        })

        // check tree structure
        final String expectedTree =
                "+ (0)\n" +          // 0 * 0
                "  +a\n" +
                "    +b (2)\n" +     // 1 * 2
                "      +c\n" +
                "        +d (8)\n" + // 2 * 4
                "        +e (12)\n" + // 3 * 4
                "    +z (8)\n";      // 4 * 2
        assertPrintTreeEquals(expectedTree, trie)
    }

    @Test
    void testRewriteTree() {
        final PrefixTree<String, Integer> trie = createSampleTree()

        trie.rootNode = trie.rewriteTree(trie.rootNode, new NodeVisitor<String, Integer>() {
            @Override
            Node<String, Integer> visit(@NotNull Node<String, Integer> node, int depth) {
                // change a tree structure
                // remove a child (to test if changes in a map are recognized by a rewriteTree())
                if (node.getKey().equals("c") && node.children().size() > 1) {
                    Iterator<Map.Entry<String, Node<String, Integer>>> iter =
                            node.children().entrySet().iterator()
                    iter.next()
                    iter.remove()
                }
                // create new instances with tripled keys (to test if key change is handled)
                Node<String, Integer> newNode = trie.nodeFactory.createNode(
                        node.getKey() + node.getKey() + node.getKey(), node.getValue())
                newNode.children().putAll(node.children()); // copy children
                return newNode
            }
        })

        // check tree structure
        final String expectedTree =
                "+ (0)\n" +
                "  +aaa\n" +
                "    +bbb (1)\n" +
                "      +ccc\n" +
                // d is removed
                "        +eee (3)\n" +
                "    +zzz (4)\n"
        assertPrintTreeEquals(expectedTree, trie)
    }
}
