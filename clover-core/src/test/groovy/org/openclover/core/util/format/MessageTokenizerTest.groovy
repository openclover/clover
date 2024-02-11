package org.openclover.core.util.format

import org.junit.Test

import static org.junit.Assert.*

/**
 * Set of test cases for the message tokenizer.
 */
class MessageTokenizerTest {

    @Test
    void testAnchorTokens() throws MessageFormatException {
        MessageTokenizer tokens = new MessageTokenizer("<A></A>")
        assertNextToken(tokens, MessageTokenizer.ANCHOR_START, "<A>")
        assertNextToken(tokens, MessageTokenizer.ANCHOR_END, "</A>")
        assertTrue(!tokens.hasNext())

        tokens = new MessageTokenizer(" </A> ")
        assertNextToken(tokens, MessageTokenizer.TEXT, " ")
        assertNextToken(tokens, MessageTokenizer.ANCHOR_END, "</A>")
        assertNextToken(tokens, MessageTokenizer.TEXT, " ")
        assertTrue(!tokens.hasNext())
    }

    @Test
    void testBoldTokens() throws MessageFormatException {
        MessageTokenizer tokens = new MessageTokenizer("<B></b>")
        assertNextToken(tokens, MessageTokenizer.BOLD_START, "<B>")
        assertNextToken(tokens, MessageTokenizer.BOLD_END, "</b>")
        assertTrue(!tokens.hasNext())
    }

    @Test
    void testHorizontalLineTokens() throws MessageFormatException {
        MessageTokenizer tokens = new MessageTokenizer(" * * ****** <a>")
        assertNextToken(tokens, MessageTokenizer.TEXT, " * * ")
        assertNextToken(tokens, MessageTokenizer.HORIZONTAL_LINE, "******")
        assertNextToken(tokens, MessageTokenizer.TEXT, " ")
        assertNextToken(tokens, MessageTokenizer.ANCHOR_START, "<a>")
        assertTrue(!tokens.hasNext())

        tokens = new MessageTokenizer("** ")
        assertNextToken(tokens, MessageTokenizer.HORIZONTAL_LINE, "**")
        assertNextToken(tokens, MessageTokenizer.TEXT, " ")
        assertTrue(!tokens.hasNext())

        tokens = new MessageTokenizer(" **")
        assertNextToken(tokens, MessageTokenizer.TEXT, " ")
        assertNextToken(tokens, MessageTokenizer.HORIZONTAL_LINE, "**")
        assertTrue(!tokens.hasNext())
    }

    private static void assertNextToken(MessageTokenizer aTokens, int aToken, String aContent)
            throws MessageFormatException {
        assertTrue(aTokens.hasNext())
        assertEquals(aToken, aTokens.nextToken())
        assertEquals(aContent, aTokens.getContent())
    }
}
