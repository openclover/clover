package com.atlassian.clover.reporters.html.source.java;

import com.atlassian.clover.reporters.html.source.SourceListener;

/**
 * Classes that are interested in aspects of Java source that might be
 * useful for rendering - new lines, comments, keywords, identifiers, string literals, imports, packages
 */
public interface JavaSourceListener extends SourceListener {
    void onPackageSegment(String packageName, String seg);
    void onImportSegment(String importedName, String seg);
    void onImport(String accum);
    void onIdentifier(String ident);
    void onStringLiteral(String s);
    void onKeyword(String s);
    void onCommentChunk(String s);
    void onJavadocTag(String s);
}
