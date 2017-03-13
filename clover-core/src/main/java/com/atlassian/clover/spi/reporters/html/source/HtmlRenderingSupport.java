package com.atlassian.clover.spi.reporters.html.source;

import com.atlassian.clover.api.registry.ClassInfo;

public interface HtmlRenderingSupport {
    String htmlEscapeStr(String aString, String tabString, String spaceString);
    String getPackageRelPath(String pkgA, String pkgB);
    String getRootRelPath(String aPkg);

    /**
     * Get a link to a source file for a given class, e.g. "com/abc/A.java#A"
     *
     * @param toplevel whether we need a link to a file from a top level (will contain package path) or not (will have
     *                 a link to file only)
     * @param withAnchor whether link should contain an anchor to the clas
     * @param classInfo class we want a link for
     * @return StringBuffer
     */
    StringBuffer getSrcFileLink(boolean toplevel, boolean withAnchor, ClassInfo classInfo);
}
