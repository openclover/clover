package org.openclover.idea.util.jdom;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;

import java.io.BufferedOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 *
 *
 */
public class JDOMUtil {

    public static Document loadDocument(InputStream inputstream)
            throws JDOMException, IOException {

        SAXBuilder saxbuilder = getBuilder();
        return saxbuilder.build(new InputStreamReader(inputstream, StandardCharsets.UTF_8));
    }

    public static Document loadDocument(File file)
            throws JDOMException, IOException {
        SAXBuilder saxbuilder = getBuilder();
        return saxbuilder.build(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    }

    public static Document loadDocument(String str)
            throws JDOMException, IOException {
        SAXBuilder saxbuilder = getBuilder();
        StringReader reader = new StringReader(str);
        return saxbuilder.build(reader);
    }

    private static SAXBuilder getBuilder() {
        SAXBuilder saxbuilder = new SAXBuilder();
        saxbuilder.setEntityResolver((s, s1) -> new InputSource(new CharArrayReader(new char[0])));
        return saxbuilder;
    }

    public static void writeDocument(Document document, File file)
            throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            writeDocument(document, bos);
        }
    }

    public static void writeDocument(Document document, OutputStream outputstream)
            throws IOException {
        writeDocument(document, new OutputStreamWriter(outputstream, StandardCharsets.UTF_8));
    }

    public static String writeDocument(Document document) throws IOException {
        StringWriter writer = new StringWriter();
        writeDocument(document, writer);
        return writer.toString();
    }

    public static byte[] printDocument(Document document)
            throws UnsupportedEncodingException, IOException {
        CharArrayWriter chararraywriter = new CharArrayWriter();
        writeDocument(document, chararraywriter);
        return (new String(chararraywriter.toCharArray())).getBytes(StandardCharsets.UTF_8);
    }

    public static void writeDocument(Document document, Writer writer)
            throws IOException {
        XMLOutputter xmloutputter = createOutputter();
        try {
            xmloutputter.output(document, writer);
            writer.close();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static boolean areElementsEqual(Element element, Element element1) {
        Document doc1 = new Document(element.clone());
        Document doc2 = new Document(element1.clone());
        CharArrayWriter cw1 = new CharArrayWriter();
        CharArrayWriter cw2 = new CharArrayWriter();
        try {
            writeDocument(doc1, cw1);
            writeDocument(doc2, cw2);
        } catch (IOException e) {
        }
        if (cw1.size() != cw2.size()) {
            return false;
        } else {
            return cw1.toString().equals(cw2.toString());
        }
    }

    public static XMLOutputter createOutputter() {
        XMLOutputter myxmloutputter = new XMLOutputter();
        Format format = Format.getPrettyFormat();
        format.setIndent("  ");
        format.setTextMode(Format.TextMode.NORMALIZE);
        myxmloutputter.setFormat(format);
        return myxmloutputter;
    }

}
