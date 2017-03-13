package com.atlassian.clover.reporters.html.source.java;

import com.atlassian.clover.reporters.html.source.SourceListener;

/**
 * Classes that are interested in aspects of Java source that might be
 * useful for rendering - new lines, comments, keywords, identifiers, string literals, imports, packages
 */
public interface JavaSourceListener extends SourceListener {
    public void onPackageSegment(String packageName, String seg);
    public void onImportSegment(String importedName, String seg);
    public void onImport(String accum);
    public void onIdentifier(String ident);
    public void onStringLiteral(String s);
    public void onKeyword(String s);
    public void onCommentChunk(String s);
    public void onJavadocTag(String s);
}
