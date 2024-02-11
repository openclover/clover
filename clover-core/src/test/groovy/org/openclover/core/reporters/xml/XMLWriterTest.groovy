package org.openclover.core.reporters.xml

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.openclover.core.util.Maps.newHashMap
import static org.openclover.core.util.Maps.newTreeMap

class XMLWriterTest {

    @Test
    void testWriteDecl() throws IOException {
        StringWriter out = new StringWriter()
        XMLWriter xml = new XMLWriter(out)
        xml.writeXMLDecl()
        xml.close()
        String expected = "<?xml version=\"1.0\"?>"
        String actual = out.toString()
        assertEquals(expected, actual)
        ByteArrayOutputStream bout = new ByteArrayOutputStream()
        OutputStreamWriter writer = new OutputStreamWriter(bout, "UTF-8")
        xml = new XMLWriter(writer)
        expected = "<?xml version=\"1.0\" encoding=\"" + writer.getEncoding() + "\"?>"
        xml.writeXMLDecl()
        xml.close()
        actual = new String(bout.toByteArray())
        assertEquals(expected, actual)
    }

    @Test
    void testWriteElements() throws IOException {
        StringWriter out = new StringWriter()
        XMLWriter xml = new XMLWriter(out)
        xml.writeElementStart("foo", newHashMap())
        xml.close()
        String expected = "<foo>"
        String actual = out.toString().trim()
        assertEquals(expected, actual)

        out = new StringWriter()
        xml = new XMLWriter(out)
        xml.writeElementStart("foo", newHashMap(), true)
        xml.close()
        expected = "<foo/>"
        actual = out.toString().trim()
        assertEquals(expected, actual)

        Map attribs = newTreeMap()

        attribs.put("key1", "value1")
        attribs.put("key2", "value2")
        attribs.put("key3", "value3")

        out = new StringWriter()
        xml = new XMLWriter(out)
        xml.writeElementStart("foo", attribs)
        xml.close()
        assertEquals("<foo key1=\"value1\" key2=\"value2\" key3=\"value3\">", out.toString().trim())

        out = new StringWriter()
        xml = new XMLWriter(out)
        xml.writeElementStart("foo", attribs, true)
        xml.close()
        assertEquals("<foo key1=\"value1\" key2=\"value2\" key3=\"value3\"/>", out.toString().trim())

        out = new StringWriter()
        xml = new XMLWriter(out)
        xml.writeElementEnd("foo")
        xml.close()
        assertEquals("</foo>", out.toString().trim())
    }

    @Test
    void testWriteText() throws IOException {
        StringWriter out = new StringWriter()
        XMLWriter xml = new XMLWriter(out)
        xml.writeText("foo")
        xml.writeElementStart("b", newHashMap())
        xml.writeText("bar")
        xml.writeElementEnd("b")
        xml.close()
        String expected = "foo<b>bar</b>"
        String actual = out.toString().trim()
        assertEquals(expected, actual)
    }

    @Test
    void testEscapeAttributeValue() {
        assertEquals("&amp;&lt;&gt;&quot;&apos;", XMLWriter.escapeAttributeValue("&<>\"'"))
        assertEquals("&amp;", XMLWriter.escapeAttributeValue("&"))
        assertEquals("&lt;", XMLWriter.escapeAttributeValue("<"))
        assertEquals("&gt;", XMLWriter.escapeAttributeValue(">"))
        assertEquals("", XMLWriter.escapeAttributeValue(""))
        assertEquals("contains(Money) : boolean", XMLWriter.escapeAttributeValue("contains(Money) : boolean"))
        assertEquals("contains(&lt;Money&gt;) : boolean", XMLWriter.escapeAttributeValue("contains(<Money>) : boolean"))
    }
}
