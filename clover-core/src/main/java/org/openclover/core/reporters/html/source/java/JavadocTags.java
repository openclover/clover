package org.openclover.core.reporters.html.source.java;

import java.util.Set;
import java.util.Collections;

import static org.openclover.core.util.Sets.newHashSet;

/** Determines if a string is a javadoc tag */
public final class JavadocTags {
    private static final Set<String> SET = Collections.unmodifiableSet(newHashSet(
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
