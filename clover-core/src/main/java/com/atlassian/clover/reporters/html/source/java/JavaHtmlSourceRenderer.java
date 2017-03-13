package com.atlassian.clover.reporters.html.source.java;

import clover.com.google.common.collect.Sets;
import clover.org.apache.commons.collections.Factory;
import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.reporters.html.source.PlaintextHtmlSourceRenderer;
import com.atlassian.clover.spi.reporters.html.source.HtmlRenderingSupport;
import com.atlassian.clover.spi.reporters.html.source.LineRenderInfo;

import java.util.Set;
import java.util.List;
import java.util.Map;

import clover.org.apache.commons.collections.map.LazyMap;

import static clover.com.google.common.collect.Maps.newHashMap;
import static com.atlassian.clover.spi.reporters.html.source.SourceReportCss.*;


/**
 * Renders Java tokens in HTML
 */
public class JavaHtmlSourceRenderer extends PlaintextHtmlSourceRenderer implements JavaSourceListener {
    protected static final String CLOSE_SPAN = "</span>";
    protected static final String OPEN_KEYWORD = "<span class=\"" + KEYWORD_CLASS + "\">";
    protected static final String CLOSE_KEYWORD = CLOSE_SPAN;
    protected static final String OPEN_STRING = "<span class=\"" + STRING_CLASS + "\">";
    protected static final String CLOSE_STRING = CLOSE_SPAN;
    protected static final String OPEN_JDOCTAG = "<span class=\"" + JAVADOC_CLASS + "\">";
    protected static final String CLOSE_JDOCTAG = CLOSE_SPAN;
    protected static final String OPEN_COMMENT = "<span class=\"" + COMMENT_CLASS + "\">";
    protected static final String CLOSE_COMMENT = CLOSE_SPAN;

    private final FullProjectInfo projectInfo;
    private final FullPackageInfo packageInfo;
    private final Set<FullPackageInfo> pkgsInScope = Sets.newHashSet();
    private final Map<String, ClassInfo> classesInScope = newHashMap();
    private final Map<String, Boolean> areTestOnlyPackages = newHashMap();

    public JavaHtmlSourceRenderer(FullFileInfo fileInfo, List<LineRenderInfo> lineInfo, HtmlRenderingSupport renderingHelper, String emptyCoverageMsg, String tab, String space) {
        super(lineInfo, renderingHelper, emptyCoverageMsg, tab, space);
        this.packageInfo = (FullPackageInfo) fileInfo.getContainingPackage();
        this.projectInfo = (FullProjectInfo) packageInfo.getContainingProject();
    }

    @Override
    public void onImport(String imp) {
        if (imp.endsWith("*")) {
            // wild card import
            String pkg = imp.substring(0, imp.lastIndexOf('.'));
            FullPackageInfo pkgInfo = (FullPackageInfo) projectInfo.getNamedPackage(pkg);
            if (pkgInfo != null) {
                pkgsInScope.add(pkgInfo);
            }
        }
        else {
            // fqcn import
            ClassInfo cInfo = projectInfo.findClass(imp);
            if (cInfo != null) {
                classesInScope.put(cInfo.getName(), cInfo);
            }
        }
    }

    private ClassInfo searchInScope(String ident) {
        // look in the cache of previously resolved classes
        ClassInfo cInfo = classesInScope.get(ident);
        if (cInfo == null) {
            // search current package
            cInfo = projectInfo.findClass(packageInfo.getName() + "." + ident);
            if (cInfo == null) {
                // search packages in scope
                for (FullPackageInfo pkgInfo : pkgsInScope) {
                    cInfo = projectInfo.findClass(pkgInfo.getName() + "." + ident);
                    if (cInfo != null) {
                        // cache the class
                        classesInScope.put(ident, cInfo);
                    }
                }
            }
        }
        return cInfo;
    }

    @Override
    public void onStringLiteral(String s) {
        out.append(OPEN_STRING);
        out.append(renderingHelper.htmlEscapeStr(s, tab, space));
        out.append(CLOSE_STRING);
    }

    @Override
    public void onKeyword(String s) {
        out.append(OPEN_KEYWORD);
        out.append(s);
        out.append(CLOSE_KEYWORD);
    }

    @Override
    public void onCommentChunk(String s) {
        out.append(OPEN_COMMENT);
        out.append(renderingHelper.htmlEscapeStr(s, tab, space));
        out.append(CLOSE_COMMENT);
    }

    @Override
    public void onJavadocTag(String s) {
        out.append(OPEN_JDOCTAG);
        out.append(s);
        out.append(CLOSE_JDOCTAG);
    }

    /**
     * Returns true if package contains only test classes
     */
    public boolean isTestOnlyPackage(final PackageInfo pkg) {
        // use a map to cache previously searched packages
        Map<String, Boolean> lazyMap = LazyMap.decorate(areTestOnlyPackages, new Factory() {
            @Override
            public Object create() {
                for (ClassInfo classInfo : pkg.getAllClasses()) {
                    if (!classInfo.isTestClass()) {
                        return Boolean.FALSE;
                    }
                }
                return Boolean.TRUE;
            }
        });

        return lazyMap.get(pkg.getName()).booleanValue();
    }

    @Override
    public void onPackageSegment(String packageName, String seg) {
        final PackageInfo pkg = projectInfo.getNamedPackage(packageName);
        if (pkg != null) {
            out.append("<a href=\"").append(renderingHelper.getPackageRelPath(packageName, packageInfo.getName()));
            // link to application classes, unless package contains test classes only
            if (isTestOnlyPackage(pkg)) {
                out.append("testsrc-");
            }
            out.append("pkg-summary.html\">");
            out.append(seg);
            out.append("</a>");
        } else {
            out.append(seg);
        }
    }

    @Override
    public void onImportSegment(String accum, String seg) {
        if (projectInfo.getNamedPackage(accum) != null) {
            out.append("<a href=\"").append(renderingHelper.getPackageRelPath(accum, packageInfo.getName()));
            out.append("pkg-summary.html\">");
            out.append(seg);
            out.append("</a>");
        } else {
            final ClassInfo clazz = projectInfo.findClass(accum);
            if (clazz != null) {
                out.append("<a href=\"").append(renderingHelper.getRootRelPath(packageInfo.getName()));
                out.append(renderingHelper.getSrcFileLink(true, true, clazz)).append("\">");
                out.append(seg);
                out.append("</a>");
            } else {
                out.append(seg);
            }
        }
    }

    @Override
    public void onIdentifier(String id) {
        final ClassInfo cInfo = searchInScope(id);
        if (cInfo != null) {
            out.append("<a href=\"").append(renderingHelper.getRootRelPath(packageInfo.getName()));
            out.append(renderingHelper.getSrcFileLink(true, true, cInfo)).append("\">");
            out.append(id);
            out.append("</a>");
        } else {
            out.append(id);
        }
    }
}