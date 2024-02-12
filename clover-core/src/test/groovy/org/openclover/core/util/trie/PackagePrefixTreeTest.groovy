package org.openclover.core.util.trie

import org.openclover.core.registry.entities.BasePackageInfo
import org.openclover.core.reporters.html.PackageInfoExt
import org.openclover.core.util.collections.Pair
import org.junit.Test

class PackagePrefixTreeTest extends PrefixTreeTest {

    @Test
    void testAdd() throws Exception {
        // note: input package does not have to be sorted alphabetically
        List<Pair<String, PackageInfoExt>> inputPackages = [
            // test: a.b.c.d is a prefix of *.java and *.groovy, has no value -> shall compact
            Pair.of("a.b.c.d",
                    (PackageInfoExt)null),
            Pair.of("a.b.c.d.java",
                    new PackageInfoExt(new BasePackageInfo(null, "a.b.c.d.java"), false) ),
            Pair.of("a.b.c.d.groovy",
                    new PackageInfoExt(new BasePackageInfo(null, "a.b.c.d.groovy"), false) ),

            // test: a.b has value, a.b.c not -> shall not compact
            Pair.of("a.b",
                    new PackageInfoExt(new BasePackageInfo(null, "a.b"), false) ),
            Pair.of("a.b.c",
                    (PackageInfoExt)null),

            // test: java.util not alphabetically - shall sort it
            Pair.of("java.util",
                    new PackageInfoExt(new BasePackageInfo(null, "java.util"), false) ),

            // test: test-only package
            Pair.of("a.b.c.html",
                    new PackageInfoExt(new BasePackageInfo(null, "a.b.c.html"), true) ),

            // test: add longer package before shorter one
            Pair.of("a.c.e.util",
                    new PackageInfoExt(new BasePackageInfo(null, "a.c.e.util"), false) ),
            Pair.of("a.c.e",
                    new PackageInfoExt(new BasePackageInfo(null, "a.c.e"), false) )
        ]

        // build a tree
        PackagePrefixTree tree = new PackagePrefixTree()
        for (Pair<String, PackageInfoExt> inputPkg : inputPackages) {
            tree.add(inputPkg.first, inputPkg.second)
        }

        // check how it looks uncompressed
        String expectedTree =
                "+\n" +
                "  +a\n" +
                "    +b (a.b false)\n" +
                "      +c\n" +
                "        +d\n" +
                "          +groovy (a.b.c.d.groovy false)\n" +
                "          +java (a.b.c.d.java false)\n" +
                "        +html (a.b.c.html true)\n" +
                "    +c\n" +
                "      +e (a.c.e false)\n" +
                "        +util (a.c.e.util false)\n" +
                "  +java\n" +
                "    +util (java.util false)\n"
        assertPrintTreeEquals(expectedTree, tree)

        // check how it looks after compression
        String expectedTreeCompressed =
                "+\n" +
                "  +a\n" +
                "    +b (a.b false)\n" +
                "      +c\n" +
                "        +d\n" +
                "          +groovy (a.b.c.d.groovy false)\n" +
                "          +java (a.b.c.d.java false)\n" +
                "        +html (a.b.c.html true)\n" +
                "    +c.e (a.c.e false)\n" +
                "      +util (a.c.e.util false)\n" +
                "  +java.util (java.util false)\n"
        tree.compressTree()
        assertPrintTreeEquals(expectedTreeCompressed, tree)
    }

    @Test
    void testCompressTreeOneNode() throws Exception {
        // note: input package does not have to be sorted alphabetically
        List<Pair<String, PackageInfoExt>> inputPackages = [
            // test: a.b.c.d is a prefix of *.java and *.groovy, has no value -> shall compact
            Pair.of("a.b.c.d.java",
                    new PackageInfoExt(new BasePackageInfo(null, "a.b.c.d.java"), false))
        ]

        // build a tree
        PackagePrefixTree tree = new PackagePrefixTree()
        for (Pair<String, PackageInfoExt> inputPkg : inputPackages) {
            tree.add(inputPkg.first, inputPkg.second)
        }

        // check how it looks uncompressed
        String expectedTree =
                "+\n" +
                "  +a\n" +
                "    +b\n" +
                "      +c\n" +
                "        +d\n" +
                "          +java (a.b.c.d.java false)\n"
        assertPrintTreeEquals(expectedTree, tree)

        // check how it looks after compression
        String expectedTreeCompressed =
                "+a.b.c.d.java (a.b.c.d.java false)\n"
        tree.compressTree()
        assertPrintTreeEquals(expectedTreeCompressed, tree)
    }

}