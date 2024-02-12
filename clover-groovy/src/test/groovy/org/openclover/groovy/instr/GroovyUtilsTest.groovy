package org.openclover.groovy.instr

import junit.framework.TestCase
import org.codehaus.groovy.ast.ASTNode

/**
 * Test for {@link org.openclover.groovy.instr.GroovyUtils}.
 */
class GroovyUtilsTest extends TestCase {

    private class ASTNodeMock extends ASTNode {
        int startLine, startCol, endLine, endCol

        ASTNodeMock(int startLine, int startCol, int endLine, int endCol) {
            this.startLine = startLine
            this.startCol = startCol
            this.endLine = endLine
            this.endCol = endCol
        }

        @Override
        int getColumnNumber() {
            startCol
        }

        @Override
        int getLastLineNumber() {
            endLine
        }

        @Override
        int getLastColumnNumber() {
            endCol
        }

        @Override
        int getLineNumber() {
            startLine
        }
    }

    /**
     * Test for {@link org.openclover.groovy.instr.GroovyUtils#hasValidSourceRegion(org.codehaus.groovy.ast.ASTNode)}
     */
    void testHasSensibleStartEndNumbering() {
        ASTNode badMinusOne = new ASTNodeMock(-1, -1, -1, -1)  // no data
        ASTNode badLines = new ASTNodeMock(100, 1, 90, 2) // start line > end line
        ASTNode badColumns = new ASTNodeMock(100, 10, 100, 0) // start column > end column, one line
        ASTNode okEmpty = new ASTNodeMock(1, 1, 1, 1) // zero-character node, weird but ok
        ASTNode okOneChar = new ASTNodeMock(10, 1, 10, 2) // one-character node
        ASTNode okOneLine = new ASTNodeMock(20, 1, 20, 22) // one-line, many chars
        ASTNode okMultiLineEqualColumns = new ASTNodeMock(90, 1, 100, 1) // multi-line, start col == end col
        ASTNode okMultiLineGreaterColumns = new ASTNodeMock(90, 20, 100, 10) // multi-line, start col > end col (yes, it's ok)
        ASTNode okMultiLineLowerColumns = new ASTNodeMock(90, 10, 100, 20) // multi-line, start col < end col

        // negative tests
        assertFalse(GroovyUtils.hasValidSourceRegion(badMinusOne))
        assertFalse(GroovyUtils.hasValidSourceRegion(badLines))
        assertFalse(GroovyUtils.hasValidSourceRegion(badColumns))

        // positive tests
        assertTrue(GroovyUtils.hasValidSourceRegion(okEmpty))
        assertTrue(GroovyUtils.hasValidSourceRegion(okOneChar))
        assertTrue(GroovyUtils.hasValidSourceRegion(okOneLine))
        assertTrue(GroovyUtils.hasValidSourceRegion(okMultiLineEqualColumns))
        assertTrue(GroovyUtils.hasValidSourceRegion(okMultiLineLowerColumns))
        assertTrue(GroovyUtils.hasValidSourceRegion(okMultiLineGreaterColumns))
    }
}
