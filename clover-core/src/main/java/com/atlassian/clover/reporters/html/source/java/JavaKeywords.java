package com.atlassian.clover.reporters.html.source.java;

import clover.com.google.common.collect.Sets;
import com.atlassian.clover.instr.java.JavaTokenTypes;

import java.util.Set;
import java.util.Collections;

/** Determins if a token id corresponds to a Java keyword */
public class JavaKeywords {
    private static final Set<Integer> SET = Collections.unmodifiableSet(Sets.newHashSet(
            JavaTokenTypes.ABSTRACT,
            JavaTokenTypes.FINAL,
            JavaTokenTypes.LITERAL_assert,
            JavaTokenTypes.LITERAL_boolean,
            JavaTokenTypes.LITERAL_break,
            JavaTokenTypes.LITERAL_byte,
            JavaTokenTypes.LITERAL_case,
            JavaTokenTypes.LITERAL_catch,
            JavaTokenTypes.LITERAL_char,
            JavaTokenTypes.LITERAL_class,
            JavaTokenTypes.LITERAL_continue,
            JavaTokenTypes.LITERAL_default,
            JavaTokenTypes.LITERAL_do,
            JavaTokenTypes.LITERAL_double,
            JavaTokenTypes.LITERAL_else,
            JavaTokenTypes.LITERAL_extends,
            JavaTokenTypes.LITERAL_false,
            JavaTokenTypes.LITERAL_finally,
            JavaTokenTypes.LITERAL_float,
            JavaTokenTypes.LITERAL_for,
            JavaTokenTypes.LITERAL_if,
            JavaTokenTypes.LITERAL_implements,
            JavaTokenTypes.LITERAL_import,
            JavaTokenTypes.LITERAL_instanceof,
            JavaTokenTypes.LITERAL_int,
            JavaTokenTypes.LITERAL_interface,
            JavaTokenTypes.LITERAL_long,
            JavaTokenTypes.LITERAL_native,
            JavaTokenTypes.LITERAL_new,
            JavaTokenTypes.LITERAL_null,
            JavaTokenTypes.LITERAL_package,
            JavaTokenTypes.LITERAL_private,
            JavaTokenTypes.LITERAL_protected,
            JavaTokenTypes.LITERAL_public,
            JavaTokenTypes.LITERAL_return,
            JavaTokenTypes.LITERAL_short,
            JavaTokenTypes.LITERAL_static,
            JavaTokenTypes.LITERAL_super,
            JavaTokenTypes.LITERAL_switch,
            JavaTokenTypes.LITERAL_synchronized,
            JavaTokenTypes.LITERAL_this,
            JavaTokenTypes.LITERAL_throw,
            JavaTokenTypes.LITERAL_throws,
            JavaTokenTypes.LITERAL_transient,
            JavaTokenTypes.LITERAL_true,
            JavaTokenTypes.LITERAL_try,
            JavaTokenTypes.LITERAL_void,
            JavaTokenTypes.LITERAL_volatile,
            JavaTokenTypes.LITERAL_while,
            JavaTokenTypes.STRICTFP
    ));

    public static boolean contains(int id) {
        return SET.contains(Integer.valueOf(id));
    }
}
