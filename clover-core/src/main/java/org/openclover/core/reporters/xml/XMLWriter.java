package org.openclover.core.reporters.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Map;

public class XMLWriter {

    private static final String NL = System.getProperty("line.separator");
    private static final String INDENT = "   ";
    private Writer out;
    private String encoding;

    private boolean pretty = true;
    private boolean inText = false;
    private int textLevel = -1;
    private int level = -1;

    public XMLWriter(OutputStream os, String encoding) throws UnsupportedEncodingException {
        this.encoding = encoding;
        if (encoding == null) {
            out = new OutputStreamWriter(os);
        }
        else {
            out = new OutputStreamWriter(os, encoding);
        }
    }

    public XMLWriter(Writer out) {
        this.out = out;
        if (out instanceof OutputStreamWriter) {
            encoding = ((OutputStreamWriter)out).getEncoding();
        }
    }

    private static final String AMP = "&";
    private static final String LT = "<";
    private static final String GT = ">";
    private static final String QUOT = "\"";
    private static final String APOS = "'";

    /**
     * Replaces &amp; &lt; &gt; ' " with their XML entities. This makes these safe for use in an XML attribute value.
     * @param str the str to escape
     * @return the escaped string
     */
    public static String escapeAttributeValue(String str) {
        return str.replaceAll(AMP, "&amp;").
                replaceAll(LT, "&lt;").
                replaceAll(GT, "&gt;").
                replaceAll(QUOT, "&quot;").
                replaceAll(APOS, "&apos;");
    }

    public void writeXMLDecl() throws IOException {
        out.write("<?xml version=\"1.0\"");
        if (encoding != null) {
            out.write(" encoding=\""+encoding+"\"");
        }
        out.write("?>");
    }


    public void writeElementStart(String name, Map<String, String> attribs) throws IOException {
        writeElementStart(name, attribs, false);
    }

    public void writeElement(String name, Map<String, String> attribs) throws IOException {
        writeElementStart(name, attribs, true);
    }
    
    public void writeElementStart(String name, Map<String, String> attribs, boolean atomic) throws IOException {
        level++;

        if (pretty && !inText) {
            out.write(NL);
            writeIndent();
        }
        out.write("<" + name);

        for (String key : attribs.keySet()) {
            String val = attribs.get(key);
            out.write(" " + key + "=\"" + val + "\"");
        }

        if (atomic) {
            level--;
            out.write("/");
        }
        out.write(">");
    }

    public void writeElementEnd(String name) throws IOException {
        if (inText) {
            if (level == textLevel) {
                inText = false;
                textLevel = -1;
            }
        }
        else if (pretty) {
            out.write(NL);
            writeIndent();
        }
        level--;
        out.write("</"+name+">");
    }

    public void writeText(String text) throws IOException {
        out.write(text);
        inText = true;
        if (textLevel == -1) {
            textLevel = level;
        }
    }

    public void close() throws IOException {
        out.flush();
        out.close();
    }

    private void writeIndent() throws IOException {
        for (int i = 0; i < level; i++) {
            out.write(INDENT);
        }
    }
}
