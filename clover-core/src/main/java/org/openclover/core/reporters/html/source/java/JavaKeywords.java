package org.openclover.core.reporters.html.source.java;

import com.atlassian.clover.instr.java.JavaTokenTypes;

import java.util.Set;
import java.util.Collections;

import static org.openclover.core.util.Sets.newHashSet;

/** Determines if a token id corresponds to a Java keyword */
public class JavaKeywords {
    private static final Set<Integer> SET = Collections.unmodifiableSet(newHashSet(
            JavaTokenTypes.ABSTRACT,
            JavaTokenTypes.FINAL,
            JavaTokenTypes.ASSERT,
            JavaTokenTypes.BOOLEAN,
            JavaTokenTypes.BREAK,
            JavaTokenTypes.BYTE,
            JavaTokenTypes.CASE,
            JavaTokenTypes.CATCH,
            JavaTokenTypes.CHAR,
            JavaTokenTypes.CLASS,
            JavaTokenTypes.CONTINUE,
            JavaTokenTypes.DEFAULT,
            JavaTokenTypes.DO,
            JavaTokenTypes.DOUBLE,
            JavaTokenTypes.ELSE,
            JavaTokenTypes.EXTENDS,
            JavaTokenTypes.FALSE,
            JavaTokenTypes.FINALLY,
            JavaTokenTypes.FLOAT,
            JavaTokenTypes.FOR,
            JavaTokenTypes.IF,
            JavaTokenTypes.IMPLEMENTS,
            JavaTokenTypes.IMPORT,
            JavaTokenTypes.INSTANCEOF,
            JavaTokenTypes.INT,
            JavaTokenTypes.INTERFACE,
            JavaTokenTypes.LONG,
            JavaTokenTypes.NATIVE,
            JavaTokenTypes.NEW,
            JavaTokenTypes.NULL,
            JavaTokenTypes.PACKAGE,
            JavaTokenTypes.PRIVATE,
            JavaTokenTypes.PROTECTED,
            JavaTokenTypes.PUBLIC,
            JavaTokenTypes.RETURN,
            JavaTokenTypes.SHORT,
            JavaTokenTypes.STATIC,
            JavaTokenTypes.SUPER,
            JavaTokenTypes.SWITCH,
            JavaTokenTypes.SYNCHRONIZED,
            JavaTokenTypes.STRICTFP,
            JavaTokenTypes.THIS,
            JavaTokenTypes.THROW,
            JavaTokenTypes.THROWS,
            JavaTokenTypes.TRANSIENT,
            JavaTokenTypes.TRUE,
            JavaTokenTypes.TRY,
            JavaTokenTypes.VOID,
            JavaTokenTypes.VOLATILE,
            JavaTokenTypes.WHILE
    ));

    public static boolean contains(int id) {
        return SET.contains(id);
    }
}
