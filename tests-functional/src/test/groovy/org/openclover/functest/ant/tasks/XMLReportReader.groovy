package org.openclover.functest.ant.tasks

import clover.org.jdom.Document
import clover.org.jdom.Element
import clover.org.jdom.JDOMException
import clover.org.jdom.input.SAXBuilder
import groovy.transform.CompileStatic
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource

import static org.openclover.core.util.Lists.newArrayList

/**
 * Parses a Clover XML report file. This is a helper class for unit tests.
 */
@CompileStatic
class XMLReportReader {

    /**
     * Parse Clover XML file
     * @param file Clover's XML report
     * @return Document document tree
     * @throws JDOMException
     * @throws IOException
     */
    Document loadDocument(File file) throws JDOMException, IOException {
        SAXBuilder saxbuilder = getBuilder()
        return saxbuilder.build(new InputStreamReader(new FileInputStream(file), "UTF-8"))
    }

    private SAXBuilder getBuilder() {
        SAXBuilder saxbuilder = new SAXBuilder()
        saxbuilder.setEntityResolver(new EntityResolver() {
            InputSource resolveEntity(String s, String s1) {
                return new InputSource(new CharArrayReader(new char[0]))
            }
        })
        return saxbuilder
    }

    /**
     * Returns list of nodes containing the &lt;file&gt; element
     *
     * @param root document root
     * @return List&lt;Element&gt
     */
    List<Element> getSourceFiles(Document root) {
        final List<Element> sourceFiles = newArrayList()

        // get packages from application
        final Element projectElem = root.getRootElement().getChild("project")
        final List<Element> packages = (List<Element>) projectElem.getChildren("package")
        if (packages != null) {
            for (Element pkg : packages) {
                final List<Element> files = (List<Element>) pkg.getChildren("file")
                if (files != null) {
                    sourceFiles.addAll(files)
                }
            }
        }

        // get packages from test
        final Element testProjectElem = root.getRootElement().getChild("testproject")
        final List<Element> testPackages = (List<Element>) testProjectElem.getChildren("package")
        if (testPackages != null) {
            for (Element pkg : testPackages) {
                final List<Element> files = (List<Element>) pkg.getChildren("file")
                if (files != null) {
                    sourceFiles.addAll(files)
                }
            }
        }

        return sourceFiles
    }

    /**
     * Returns base name (like 'Foo.java') of the source file (the &lt;file name="..."&gt; attribute)
     */
    String getSourceFileName(Element sourceFile) {
        return sourceFile.getAttribute("name").getValue()
    }

    /**
     * Returns list of source lines (the &lt;line&gt; element) with hit count value (the 'count' attribute) for each of
     * them for given input <code>sourceFile</code> (the &lt;file&gt; element)
     *
     * @param sourceFile
     * @return int[]
     */
    int[] getHitCountsForSourceFile(Element sourceFile) {
        // as <line> nubmers are not continuous, find the max line number
        List<Element> lines = (List<Element>) sourceFile.getChildren("line")
        int maxLineNo = 0
        for (Element line : lines) {
            int lineNo = Integer.valueOf(line.getAttribute("num").getValue())
            maxLineNo = Math.max(maxLineNo, lineNo)
        }

        // now create arra with proper size, filled with zeroes
        final int[] hits = new int[maxLineNo + 1]
        for (Element line : lines) {
            // and set hits for some lines
            int lineNo = Integer.valueOf(line.getAttribute("num").getValue())
            if (line.getAttribute("count") != null) {
                // statement / method coverage
                hits[lineNo] = Integer.valueOf(line.getAttribute("count").getValue())
            } else {
                // branch coverage
                hits[lineNo] = Integer.valueOf(line.getAttribute("truecount").getValue()) +
                        Integer.valueOf(line.getAttribute("falsecount").getValue())
            }

        }

        return hits
    }

}
