package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.stmt.EmptyStatement;

/**
 * A non-singleton, mutable subclass of EmptyStatement used to represent synthetic default
 * branches injected by Groovyc for switch expressions that lack an explicit default.
 *
 * The singleton EmptyStatement.INSTANCE has valid setters (inherited from ASTNode) but must
 * not be mutated because it is shared across the AST. This subclass is created fresh for each
 * injected default so that synthesized source coordinates can be assigned safely.
 *
 * Because this extends EmptyStatement, Groovy's own code-generation passes that check
 * "x instanceof EmptyStatement" continue to work correctly.
 */
public class MutableEmptyStatement extends EmptyStatement {

    public MutableEmptyStatement(int line, int col, int lastLine, int lastCol) {
        setLineNumber(line);
        setColumnNumber(col);
        setLastLineNumber(lastLine);
        setLastColumnNumber(lastCol);
    }
}
