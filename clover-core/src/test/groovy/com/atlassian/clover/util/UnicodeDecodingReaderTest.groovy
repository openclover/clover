package com.atlassian.clover.util

import clover.antlr.Token
import clover.antlr.TokenStreamException
import clover.com.google.common.collect.Lists
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig
import com.atlassian.clover.cfg.instr.java.SourceLevel
import com.atlassian.clover.instr.java.JavaLexer
import com.atlassian.clover.instr.java.JavaTokenTypes
import org.junit.Test

import static org.junit.Assert.*

/**
 * Test of {@link com.atlassian.clover.instr.java.JavaLexer} against unicode characters
 */
class UnicodeDecodingReaderTest {

    /**
     * Helper class to verify expected tokens in a stream
     */
    static class Tok {
        final int type
        final String text
        Tok(int type, String text) {
            this.type = type
            this.text = text
        }
        /** Whitespace */
        static Tok WS(String text) {
            new Tok(JavaTokenTypes.WS, text)
        }
        /** Single line comment */
        static Tok SL(String text) {
            new Tok(JavaTokenTypes.SL_COMMENT, text)
        }
        /** Multi line comment */
        static Tok ML(String text) {
            new Tok(JavaTokenTypes.ML_COMMENT, text)
        }
        /** Identifier */
        static Tok IDENT(String text) {
            new Tok(JavaTokenTypes.IDENT, text)
        }
        /** Equal sign */
        static Tok ASSIGN() {
            new Tok(JavaTokenTypes.ASSIGN, "=")
        }
        /** String literal */
        static Tok STR(String text) {
            new Tok(JavaTokenTypes.STRING_LITERAL, text)
        }

    }

    /**
     * This is a test of a workaround with the backslash handling - unicode escape sequence is not translated to
     * a '\' character. This is to avoid a problem when a backslash is used to quote subsequent character(s), such as:
     * single quote (\'), double quote (\"), unicode escape sequence (\u1234).
     */
    @Test
    void testThatEscapedBackslashIsNotDecoded() throws Exception {
        // another example from jdk8 javadocs (java.util.Properties)
        verifyString(
                "/**\n" +
                " * ({@code ' '}, {@code '\\u005Cu0020'}), tab\n" + // a u005C stands for a backslash
                "*/ abc\\u017Cdef",
                "/**\n" +
                " * ({@code ' '}, {@code '\\u005Cu0020'}), tab\n" + // a u005C stands for a backslash
                "*/ abc\u017Cdef")
    }

    /**
     * This is a test of a workaround with CR/LF handling - unicode escaped sequences for CR/LF are not translated to
     * CR/LF characters in order to avoid a problem that extra source lines are added (and registered in a database)
     * which may lead to desynchronization of lines in code editors like Eclipse/IDEA (see CLOV-1131).
     *
     * @throws Exception
     */
    @Test
    void testThatEscapedCRLFAreNotDecoded() throws Exception {
        // example from CLOV-1131
        verifyString("/** {@code \\u000a} LF */")
        verifyString("/** {@code \\u000d} CR */")
    }

    /**
     * Test for unicode escape sequences and characters in comments.
     */
    @Test
    void testThatCommentsAreDecoded() throws Exception {
        // u0073='s', u017C='ż', escaped and raw
        verifyString("/* \\u0073 */ abc", [ Tok.ML("/* s */"), Tok.WS(" "), Tok.IDENT("abc") ] as Tok[])
        verifyString("abc // \u017C", [ Tok.IDENT("abc"), Tok.WS(" "), Tok.SL("// \u017C") ] as Tok[])
    }

    /**
     * Test for unicode in character literals.
     */
    @Test
    void testThatCharLiteralsAreDecoded() throws Exception {
        // \u0073 = s, escaped
        verifyString("char s = '\\u0073';", "char s = 's';")
        // \u017C = ż, raw
        verifyString("char z_with_dot = '\u017C';")
    }

    /**
     * Test for unicode in string literals.
     */
    @Test
    void testThatStringLiteralsNotDecoded() throws Exception {
        // String turtle = "\u017C\u00F3\u0142\u0077" (escaped 'żółw')
        verifyString("String turtle=\"\\u017C\\u00F3\\u0142\\u0077\"",
                [ Tok.IDENT("String"), Tok.WS(" "), Tok.IDENT("turtle"), Tok.ASSIGN(), Tok.STR("\"\u017C\u00F3\u0142\u0077\"")] as Tok[])

        // String turtle = "żółw" (raw)
        verifyString("String turtle=\"\u017C\u00F3\u0142\u0077\"",
                [ Tok.IDENT("String"), Tok.WS(" "), Tok.IDENT("turtle"), Tok.ASSIGN(), Tok.STR("\"\u017C\u00F3\u0142\u0077\"") ] as Tok[])
    }

    /**
     * Unicode characters in token.
     */
    @Test
    void testThatTokensAreDecoded() throws Exception {
        // Animal żółw=x (raw)
        verifyString("Animal \u017C\u00F3\u0142\u0077=x",
                [ Tok.IDENT("Animal"), Tok.WS(" "), Tok.IDENT("\u017C\u00F3\u0142\u0077"), Tok.ASSIGN(), Tok.IDENT("x") ] as Tok[])

        // Animal żółw=x (escaped)
        verifyString("Animal \\u017C\\u00F3\\u0142\\u0077=x",
                [ Tok.IDENT("Animal"), Tok.WS(" "), Tok.IDENT("\u017C\u00F3\u0142\u0077"), Tok.ASSIGN(), Tok.IDENT("x") ] as Tok[])
    }

    /**
     * Various cases according to Java Language Specification
     */
    @Test
    void testNormalDecoding() throws Exception {
        // utf chars -> utf chars
        verifyString("sanity", "sanity")
        // escaped sequences -> utf chars
        verifyString("\\u0073\\u0061\\u006e\\u0069\\u0074\\u0079", "sanity")
        // multiple 'u' is allowed after '\'
        verifyString("\\uu0073\\uuuu0061\\uuuuu006e\\uuuuuuu0069\\u0074\\u0079", "sanity")
        // escape sequence can be used in a middle of token
        verifyString("s\\u0061nity", "sanity")
        // odd/even number of '\' characters matters (decode or not)
        verifyString("\\\\u2297","\\\\u2297")
        verifyString("\\\\\\sanity","\\\\\\sanity")
        verifyString("\\u0009","\t")
    }

    /**
     * Test how invalid escape sequences are handled
     */
    @Test
    void testDecodingErrors() throws Exception {
        verifyString("\\uNONO");   //bad hex
        verifyString("blah\\uNONOblah");  // bad hex
        verifyString("\\u");     // no hex
        verifyString("\\u000"); // incomplete hex
    }

    private void verifyString(String input) throws IOException, TokenStreamException {
        assertEquals(input, decodeString(input))
    }

    private void verifyString(String input, String expected) throws IOException, TokenStreamException {
        assertEquals(expected, decodeString(input))
    }

    /**
     * Decode input string using UnicodeDecodingReader
     */
    private String decodeString(String input) throws IOException, TokenStreamException {
        Reader reader = new UnicodeDecodingReader(new StringReader(input))
        StringWriter out = new StringWriter()
        int c = reader.read()
        while (c >= 0) {
            out.write(c)
            c = reader.read()
        }
        out.close()
        out.toString()
    }

    private void verifyString(String input, Tok[] expected) throws IOException, TokenStreamException {
        assertTokensEqual(expected, decodeStringToTokens(input))
    }

    private void assertTokensEqual(final Tok[] expected, final Token[] actual) {
        assertEquals("Number of tokens is different", expected.length, actual.length)
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Token type does not match for #" + i, expected[i].type, actual[i].getType())
            assertEquals("Token text does not match for #" + i, expected[i].text, actual[i].getText())
        }
    }

    /**
     * Decode input string using UnicodeDecodingReader and JavaLexer
     */
    private Token[] decodeStringToTokens(String input) throws IOException, TokenStreamException {
        // create lexer to parse input string
        final JavaInstrumentationConfig config = new JavaInstrumentationConfig()
        config.setSourceLevel(SourceLevel.JAVA_9)
        config.setEncoding("UTF-8")
        final JavaLexer lexer = new JavaLexer(new UnicodeDecodingReader(new StringReader(input)), config)

        // get text from all tokens and concatenate it
        final ArrayList<Token> tokens = Lists.newArrayList()
        Token token
        for (token = lexer.nextToken(); token.getType() != Token.EOF_TYPE; token = lexer.nextToken()) {
            tokens.add(token)
        }
        tokens.toArray(new Token[tokens.size()])
    }
}
