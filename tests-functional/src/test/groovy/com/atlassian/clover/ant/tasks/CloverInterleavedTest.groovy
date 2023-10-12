package com.atlassian.clover.ant.tasks

import org.hamcrest.CoreMatchers

import static org.hamcrest.MatcherAssert.assertThat

class CloverInterleavedTest extends CloverBuildFileTestBase {
    CloverInterleavedTest(String name) {
        super(name)
    }

    String getAntFileName() {
        return "clover-interleaved-instr.xml"
    }

    void testConsoleReporter() throws Exception {
        executeTarget("interleavedCompileAndTest")
        executeTarget("consoleReport")
        final String output = getProject().getProperty("report.output")
        assertTrue(output.contains("Methods: 2/2 (100%)"))
        assertTrue(output.contains("Statements: 2/2 (100%)"))
    }

    void testHtmlReporter() throws Exception {
        executeTarget("interleavedCompileAndTest")
        executeTarget("htmlReport")
        final String pkgAppHtml = getProject().getProperty("pkgAppHtml")
        assertThat(pkgAppHtml, CoreMatchers.containsString(
                "<span class=\"sortValue\">AppClass1</span>\n" +
                "                    <a href=\"AppClass1.html\" title=\"AppClass1\">AppClass1</a>\n" +
                "                    </td>\n" +
                "\n" +
                "                \n" +
                "        <td class=\" \" align=\"right\">\n" +
                "            <span class=\"sortValue\">1.0</span>\n" +
                "            100%\n" +
                "        </td>"
        ))
        assertThat(pkgAppHtml, CoreMatchers.containsString(
                "<span class=\"sortValue\">AppClass2</span>\n" +
                "                    <a href=\"AppClass2.html\" title=\"AppClass2\">AppClass2</a>\n" +
                "                    </td>\n" +
                "\n" +
                "                \n" +
                "        <td class=\" \" align=\"right\">\n" +
                "            <span class=\"sortValue\">1.0</span>\n" +
                "            100%\n" +
                "        </td>"
        ))
    }
}
