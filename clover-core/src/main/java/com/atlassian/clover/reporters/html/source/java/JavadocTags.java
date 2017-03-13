package com.atlassian.clover.reporters.html.source.java;

import clover.com.google.common.collect.Sets;

import java.util.Set;
import java.util.Collections;

/** Determines if a string is a javadoc tag */
public final class JavadocTags {
    private static final Set<String> SET = Collections.unmodifiableSet(Sets.newHashSet(
            "author",
            "version",
            "see",
            "since",
            "deprecated",
            "param",
            "return",
            "exception",
            "value",
            "serial",
            "inheritDoc",
            "link",
            "linkplain",
            "docRoot",
            "throws"));

    public static boolean contains(String candidate) {
        return SET.contains(candidate);
    }
}
