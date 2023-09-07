package com.atlassian.clover.instr.groovy;

import com.atlassian.clover.registry.FixedSourceRegion;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ClassHelper;

public class GroovyUtils {
    public static FixedSourceRegion newRegionFor(ASTNode statement) {
        return newRegionFor(statement, false);
    }

    public static FixedSourceRegion newRegionFor(ASTNode statement, boolean evenIfUnreportable) {
        if (isReportable(statement) || evenIfUnreportable) {
            return new FixedSourceRegion(statement.getLineNumber(), statement.getColumnNumber(), statement.getLastLineNumber(), statement.getLastColumnNumber());
        } else {
            return null;
        }
    }

    public static boolean isReportable(ASTNode node) {
        //Enum classes currently have no line/col information but their non-syntetic contents do.
        //We still need to instrument them

        return
            isEnum(node)
            || hasValidSourceRegion(node);
    }

    private static boolean isEnum(ASTNode node) {
        return (node instanceof ClassNode && ((ClassNode)node).isDerivedFrom(ClassHelper.Enum_Type));
    }

    /**
     * Returns true if given AST node looks to have correct source region definition: <br/>
     *  1) first/last line/column number is >= 1 <br/>
     *  2) last line >= first line <br/>
     *  3) in case of single line node, also last column >= first column <br/>
     * @return boolean - true if source region looks fine, false otherwise
     */
    public static boolean hasValidSourceRegion(ASTNode node) {
        return node.getLineNumber() >= 1
                && node.getColumnNumber() >= 1
                && ( node.getLastLineNumber() > node.getLineNumber()
                     || (node.getLastLineNumber() == node.getLineNumber()
                         && node.getLastColumnNumber() >= node.getColumnNumber()) );
    }

    public static boolean isScriptClass(ASTNode node) {
        return (node instanceof ClassNode && ((ClassNode)node).getSuperClass().getName().equals("groovy.lang.Script"));
    }
}
