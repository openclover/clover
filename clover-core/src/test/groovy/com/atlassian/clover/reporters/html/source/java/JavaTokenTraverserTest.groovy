package com.atlassian.clover.reporters.html.source.java

import clover.antlr.TokenStreamException
import com.atlassian.clover.util.UnicodeDecodingReader
import org.junit.Test

import static org.hamcrest.CoreMatchers.anyOf
import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class JavaTokenTraverserTest {

    @Test
    void testNewLineHandlingInComments() throws TokenStreamException {
        List<Chunk> chunks = [
            new Comment("/**"),
            new NewLine(),
            new Comment("*"),
            new NewLine(),
            new Comment("*/")
        ]
        checkRenderingAgainstChunkList("/**\n*\n*/", chunks)
        checkRenderingAgainstChunkList("/**\r\n*\r\n*/", chunks)
        checkRenderingAgainstChunkList("/**\r\n*\r*/", chunks)
        checkRenderingAgainstChunkList("/**\r*\r*/", chunks)
    }

    @Test
    void testBasicSourceRendering() throws TokenStreamException {
        List<Chunk> chunks = [
            new Keyword("package"),
            new Chunk(" "),
            new Chunk("com"),
            new Chunk("."),
            new Chunk("foo"),
            new Chunk("."),
            new Chunk("bar"),
            new Chunk(";"),
            new NewLine(),
            new Comment("/**"),
            new NewLine(),
            new Comment("* "),
            new JavadocTag("@author"),
            new Comment(" Harry Highpants"),
            new NewLine(),
            new Comment("**/"),
            new NewLine(),
            new Keyword("public"),
            new Chunk(" "),
            new Keyword("class"),
            new Chunk(" "),
            new Chunk("Foo"),
            new Chunk(" "),
            new Keyword("extends"),
            new Chunk(" "),
            new Chunk("Bar"),
            new Chunk(" "),
            new Chunk("{"),
            new NewLine(),
            new NewLine(),
            new Keyword("public"),
            new Chunk(" "),
            new Keyword("static"),
            new Chunk(" "),
            new Keyword("final"),
            new Chunk(" "),
            new Chunk("String"),
            new Chunk(" "),
            new Chunk("="),
            new Chunk(" "),
            new StringLiteral("\"A String\""),
            new Chunk(";"),
            new NewLine(),
            new Chunk("}"),
            new NewLine(),
        ]

        checkRenderingAgainstChunkList(chunks)
    }

    @SuppressWarnings("unchecked") // for AnyOf.anyOf()
    @Test
    void testThatNewLineInvalidatesStartOfStringLiteral() {
        List<Chunk> chunks = [
            new Chunk("\""),
            new NewLine(),
            new Chunk("\"")
        ]
        try {
            // newline character is not allowed inside " "
            checkRenderingAgainstChunkList("\"\n\"", chunks)
            checkRenderingAgainstChunkList("\"\r\"", chunks)
            fail("Should not have reached here with an invalid token.")
        } catch (TokenStreamException e) {
            assertThat(e.getMessage(), equalTo("unexpected char: '\"'"))
        }
    }

    @SuppressWarnings("unchecked") // for AnyOf.anyOf()
    @Test
    void testThatNewLineInvalidatesStartOfCharacterLiteral() {
        List<Chunk> chunks = [
            new Chunk("'"),
            new NewLine(),
            new Chunk("'")
        ]
        try {
            // newline character is not allowed inside ' '
            checkRenderingAgainstChunkList("'\n'", chunks)
            checkRenderingAgainstChunkList("'\r'", chunks)
            fail("Should not have reached here with an invalid token.")
        } catch (TokenStreamException e) {
            assertThat(e.getMessage(), anyOf(
                    equalTo("unexpected char: 0xA"),
                    equalTo("unexpected char: 0xD")))
        }
    }

    @Test
    void testThatExtendedUnicodeCharactersAreHandled() {
        try {
            List<Chunk> sunChunks = [
                // a valid double unicode character - 0x0001F31E  - shining sun
                new StringLiteral("\"\u0001\uF31E\"")
            ]
            checkRenderingAgainstChunkList(sunChunks)
        } catch (TokenStreamException ex) {
            fail(ex.getMessage())
        }
    }

    /**
     * We're using a simplified approach here - is someone declares invalid unicode character, it will be catched
     * by javac compiler. So Clover will just pass it further.
     */
    @Test
    void testThatInvalidExtendedUnicodeCharactersAreIgnored() {
        try {
            List<Chunk> invalidChunks = [
                // an invalid character (out of code point range 0x000000..0X10FFFF, which is being encoded using
                // high surrogate 0xD800..0xDBFF and low surrogate 0xDC00..0xDFFF)
                new StringLiteral("\"\uDCCC\uDFFF\""),
            ]
            checkRenderingAgainstChunkList(invalidChunks)
        } catch (TokenStreamException ex) {
            fail(ex.getMessage())
        }
    }

    @Test
    void testCCD339() throws TokenStreamException {
        List<Chunk> chunks = [
            new Comment("/**"),
            new NewLine(),
            new Comment("*"),
            new Comment("@notatag"),
            new NewLine(),
            new Comment("*/")
        ]
        checkRenderingAgainstChunkList(chunks)

        chunks = [
            new Comment("/**"),
            new NewLine(),
            new Comment("*"),
            new JavadocTag("@author"), 
            new Comment(" <a href=\"mailto:harry"),
            new Comment("@highpants.com\">Harry HighPants</a>"),
            new NewLine(),
            new Comment("*/"),
        ]
        checkRenderingAgainstChunkList(chunks)

        // from checkstyle-4.2/com/puppycrawl/tools/checkstyle/checks/blocks/LeftCurlyCheck.java
        chunks = [
            new Comment("/**"),
            new NewLine(),
            new Comment(" * <p>"),
            new NewLine(),
            new Comment(" * Checks the placement of left curly braces on types, methods and"),
            new NewLine(),
            new Comment(" * other blocks:"),
            new NewLine(),
            new Comment(" *  {"),
            new JavadocTag("@link"),
            new Comment(" TokenTypes#LITERAL_CATCH LITERAL_CATCH},  {"),
            new JavadocTag("@link"),
            new NewLine(),
            new Comment(" * TokenTypes#LITERAL_DO LITERAL_DO},  {"),
            new JavadocTag("@link"),
            new Comment(" TokenTypes#LITERAL_ELSE"),
            new NewLine(),
            new Comment(" * LITERAL_ELSE},  {"),
            new JavadocTag("@link"),
            new Comment(" TokenTypes#LITERAL_FINALLY LITERAL_FINALLY},  {"),
            new JavadocTag("@link"),
            new NewLine(),
            new Comment(" * TokenTypes#LITERAL_FOR LITERAL_FOR},  {"),
            new JavadocTag("@link"),
            new Comment(" TokenTypes#LITERAL_IF"),
            new NewLine(),
            new Comment(" * LITERAL_IF},  {"),
            new JavadocTag("@link"),
            new Comment(" TokenTypes#LITERAL_SWITCH LITERAL_SWITCH},  {"),
            new NewLine(),
            new Comment(" */")
        ]
        checkRenderingAgainstChunkList(chunks)
    }

    private void checkRenderingAgainstChunkList(final List<Chunk> chunks) throws TokenStreamException {
        checkRenderingAgainstChunkList(null, chunks)
    }

    private void checkRenderingAgainstChunkList(String src, final List<Chunk> chunks) throws TokenStreamException {
        final boolean [] endReached = [ false ]

        JavaSourceListener renderer = new JavaSourceListener() {
            private int cursor = 0

            private void checkTypeAndVal(Class actual, String s) {
                Chunk c = chunks.get(cursor++)
                assertEquals("chunk $cursor of ${chunks.size()}: ", c.getClass(), actual)
                assertEquals("chunk $cursor of ${chunks.size()}: ", c.toString(), s)
            }

            void onStringLiteral(String s) {
                checkTypeAndVal(StringLiteral.class, s)
            }

            void onKeyword(String s) {
                checkTypeAndVal(Keyword.class, s)
            }

            void onCommentChunk(String s) {
                checkTypeAndVal(Comment.class, s)
            }

            void onJavadocTag(String s) {
                checkTypeAndVal(JavadocTag.class, s)
            }

            void onNewLine() {
                checkTypeAndVal(NewLine.class, "\n")
            }

            void onPackageSegment(String accum, String seg) {
                onChunk(seg)
            }

            void onImportSegment(String accum, String seg) {
                onChunk(seg)
            }

            void onIdentifier(String ident) {
                onChunk(ident)
            }

            void onEndDocument() {
                endReached[0] = true
                assertEquals(chunks.size(), cursor);  // check that all chunks have been processed
            }

            void onChunk(String s) {
                checkTypeAndVal(Chunk.class, s)
            }

            void onStartDocument() { }
            void onImport(String accum) { }

        }

        if (src == null) {
            StringBuilder input = new StringBuilder()
            for (Chunk chunk : chunks) {
                input.append(chunk)
            }
            src = input.toString()
        }

        new JavaTokenTraverser().traverse(new UnicodeDecodingReader(new StringReader(src)), null, renderer)
        assertTrue("No endDocument event - bad input", endReached[0])
    }

    class Chunk {
        private String s

        Chunk(String s) {
            this.s = s
        }

        String toString() {
            return s
        }
    }

    class StringLiteral extends Chunk {
        StringLiteral(String s) {
            super(s)
        }
    }

    class Keyword extends Chunk {
        Keyword(String s) {
            super(s)
        }
    }

    class Comment extends Chunk {
        Comment(String s) {
            super(s)
        }
    }

    class JavadocTag extends Chunk {
        JavadocTag(String s) {
            super(s)
        }
    }

    class NewLine extends Chunk {
        NewLine() {
            super("\n")
         }
     }
}
