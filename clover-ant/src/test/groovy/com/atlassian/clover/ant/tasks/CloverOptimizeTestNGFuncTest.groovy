package com.atlassian.clover.ant.tasks

import org.w3c.dom.Document
import org.xml.sax.SAXException

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

class CloverOptimizeTestNGFuncTest extends CloverOptimizeTestBase {
    CloverOptimizeTestNGFuncTest(String name) {
        super(name, "runTestNGTests", Collections.<String, String>emptyMap())
    }

    protected void expectNoTestRunResults(String cycle) throws Exception {
        File testOutputDir = new File(util.getWorkDir().getAbsolutePath(), cycle)
        assertTrue(
            testOutputDir.toString() + " directory should exist",
            testOutputDir.exists() && testOutputDir.isDirectory() && testOutputDir.canRead())
        File resultsFile = new File(testOutputDir, "testng-results.xml")
        assertTrue(
            resultsFile.toString() + " file should exist and be readable",
            resultsFile.exists() && resultsFile.isFile() && resultsFile.canRead())

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance()
        DocumentBuilder builder = docFactory.newDocumentBuilder()
        Document doc = builder.parse(resultsFile)

        XPathFactory xpathFactory = XPathFactory.newInstance()
        XPath xpath = xpathFactory.newXPath()
        Number elementCount = (Number)xpath.evaluate("count(//testng-results/suite[@name='Ant suite']/test[@name='Ant test']/class)", doc, XPathConstants.NUMBER)
        assertEquals("No test classes should be present in the the suite for " + cycle, 0, elementCount.intValue())
    }

    protected void expectTestsRunResults(String cycle, Map<String, String[]> results) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        File testOutputDir = new File(util.getWorkDir().getAbsolutePath(), cycle)
        assertTrue(
            testOutputDir.toString() + " directory should exist",
            testOutputDir.exists() && testOutputDir.isDirectory() && testOutputDir.canRead())
        File resultsFile = new File(testOutputDir, "testng-results.xml")
        assertTrue(
            resultsFile.toString() + " file should exist and be readable",
            resultsFile.exists() && resultsFile.isFile() && resultsFile.canRead())

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance()
        DocumentBuilder builder = docFactory.newDocumentBuilder()
        Document doc = builder.parse(resultsFile)

        XPathFactory xpathFactory = XPathFactory.newInstance()
        XPath xpath = xpathFactory.newXPath()
        Number elementCount = (Number)xpath.evaluate(
                "count(//testng-results/suite[@name='Ant suite']/test[@name='Ant test']/class)",
                doc,
                XPathConstants.NUMBER)
        assertEquals("Wrong number of test classes in the suite for " + cycle, results.size(), elementCount.intValue())

        for (Map.Entry<String, String[]> entry : results.entrySet()) {
            String testName = entry.getKey()
            assertTrue(
                    "Test class " + testName + " was not run for " + cycle,
                    ((Boolean) xpath.evaluate(
                            "count(//testng-results/suite[@name='Ant suite']/test[@name='Ant test']/class[@name='" + testName + "']) = 1",
                            doc,
                            XPathConstants.BOOLEAN)).booleanValue())
        }
    }

    void testOptimizedCIBuildCycles() throws Exception {
        initialSourceCreation()
        buildThenRun(0)

        expectTestsRunResults(
            cycle(0),
            [
                    "AppClass2Test" : ["AppClass2", cycle(0)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(0), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[],
                    "NoAppClassTest" : [] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(1)

        expectNoTestRunResults(cycle(1))

        buildComplete()
        sourceChange()
        buildThenRun(2)

        expectTestsRunResults(
            cycle(2),
            [
                    "AppClass2Test" : ["AppClass2", cycle(2)] as String[],
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(0), "AppClass4", cycle(0)] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(3)

        expectTestsRunResults(
            cycle(3),
            [
                    "AppClass23Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3)] as String[],
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(0)] as String[]
            ])

        buildComplete()
        sourceChange()
        buildThenRun(4)

        expectTestsRunResults(
            cycle(4),
            [
                    "AppClass234Test" : ["AppClass2", cycle(2), "AppClass3", cycle(3), "AppClass4", cycle(4)] as String[]
            ])
    }

    protected String getTestSourceBaseName() {
        return "CloverOptimizeTestNGTest"
    }
}
