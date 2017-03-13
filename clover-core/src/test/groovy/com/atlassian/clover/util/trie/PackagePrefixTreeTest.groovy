package com.atlassian.clover.util.trie

import com.atlassian.clover.registry.entities.BasePackageInfo
import com.atlassian.clover.reporters.html.PackageInfoExt
import com.atlassian.clover.util.collections.Pair
import org.junit.Test

class PackagePrefixTreeTest extends PrefixTreeTest {

    @Test
    void testAdd() throws Exception {
        // note: input package does not have to be sorted alphabetically
        List<Pair<String, PackageInfoExt>> inputPackages = [
            // test: com.atlassian.clover.instr is a prefix of *.java and *.groovy, has no value -> shall compact
            Pair.of("com.atlassian.clover.instr",
                    (PackageInfoExt)null),
            Pair.of("com.atlassian.clover.instr.java",
                    new PackageInfoExt(new BasePackageInfo(null, "com.atlassian.clover.instr.java"), false) ),
            Pair.of("com.atlassian.clover.instr.groovy",
                    new PackageInfoExt(new BasePackageInfo(null, "com.atlassian.clover.instr.groovy"), false) ),

            // test: com.atlassian has value, com.atlassian.clover not -> shall not compact
            Pair.of("com.atlassian",
                    new PackageInfoExt(new BasePackageInfo(null, "com.atlassian"), false) ),
            Pair.of("com.atlassian.clover",
                    (PackageInfoExt)null),

            // test: java.util not alphabetically - shall sort it
            Pair.of("java.util",
                    new PackageInfoExt(new BasePackageInfo(null, "java.util"), false) ),

            // test: test-only package
            Pair.of("com.atlassian.clover.html",
                    new PackageInfoExt(new BasePackageInfo(null, "com.atlassian.clover.html"), true) ),

            // test: add longer package before shorter one
            Pair.of("com.cenqua.clover.util",
                    new PackageInfoExt(new BasePackageInfo(null, "com.cenqua.clover.util"), false) ),
            Pair.of("com.cenqua.clover",
                    new PackageInfoExt(new BasePackageInfo(null, "com.cenqua.clover"), false) )
        ]

        // build a tree
        PackagePrefixTree tree = new PackagePrefixTree()
        for (Pair<String, PackageInfoExt> inputPkg : inputPackages) {
            tree.add(inputPkg.first, inputPkg.second)
        }

        // check how it looks uncompressed
        String expectedTree =
                "+\n" +
                "  +com\n" +
                "    +atlassian (com.atlassian false)\n" +
                "      +clover\n" +
                "        +html (com.atlassian.clover.html true)\n" +
                "        +instr\n" +
                "          +groovy (com.atlassian.clover.instr.groovy false)\n" +
                "          +java (com.atlassian.clover.instr.java false)\n" +
                "    +cenqua\n" +
                "      +clover (com.cenqua.clover false)\n" +
                "        +util (com.cenqua.clover.util false)\n" +
                "  +java\n" +
                "    +util (java.util false)\n"
        assertPrintTreeEquals(expectedTree, tree)

        // check how it looks after compression
        String expectedTreeCompressed =
                "+\n" +
                "  +com\n" +
                "    +atlassian (com.atlassian false)\n" +
                "      +clover\n" +
                "        +html (com.atlassian.clover.html true)\n" +
                "        +instr\n" +
                "          +groovy (com.atlassian.clover.instr.groovy false)\n" +
                "          +java (com.atlassian.clover.instr.java false)\n" +
                "    +cenqua.clover (com.cenqua.clover false)\n" +
                "      +util (com.cenqua.clover.util false)\n" +
                "  +java.util (java.util false)\n"
        tree.compressTree()
        assertPrintTreeEquals(expectedTreeCompressed, tree)
    }

    @Test
    void testCompressTreeOneNode() throws Exception {
        // note: input package does not have to be sorted alphabetically
        List<Pair<String, PackageInfoExt>> inputPackages = [
            // test: com.atlassian.clover.instr is a prefix of *.java and *.groovy, has no value -> shall compact
            Pair.of("com.atlassian.clover.instr.java",
                    new PackageInfoExt(new BasePackageInfo(null, "com.atlassian.clover.instr.java"), false))
        ]

        // build a tree
        PackagePrefixTree tree = new PackagePrefixTree()
        for (Pair<String, PackageInfoExt> inputPkg : inputPackages) {
            tree.add(inputPkg.first, inputPkg.second)
        }

        // check how it looks uncompressed
        String expectedTree =
                "+\n" +
                "  +com\n" +
                "    +atlassian\n" +
                "      +clover\n" +
                "        +instr\n" +
                "          +java (com.atlassian.clover.instr.java false)\n"
        assertPrintTreeEquals(expectedTree, tree)

        // check how it looks after compression
        String expectedTreeCompressed =
                "+com.atlassian.clover.instr.java (com.atlassian.clover.instr.java false)\n"
        tree.compressTree()
        assertPrintTreeEquals(expectedTreeCompressed, tree)
    }

}