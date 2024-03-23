package org.openclover.core.reporters.html.source.java;

import clover.org.apache.commons.collections.map.LazyMap;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.reporters.html.source.PlaintextHtmlSourceRenderer;
import org.openclover.core.spi.reporters.html.source.HtmlRenderingSupport;
import org.openclover.core.spi.reporters.html.source.LineRenderInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openclover.core.spi.reporters.html.source.SourceReportCss.COMMENT_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.JAVADOC_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.KEYWORD_CLASS;
import static org.openclover.core.spi.reporters.html.source.SourceReportCss.STRING_CLASS;
import static org.openclover.core.util.Maps.newHashMap;
import static org.openclover.core.util.Sets.newHashSet;

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

    private final ProjectInfo projectInfo;
    private final PackageInfo packageInfo;
    private final Set<PackageInfo> pkgsInScope = newHashSet();
    private final Map<String, ClassInfo> classesInScope = newHashMap();
    private final Map<String, Boolean> areTestOnlyPackages = newHashMap();

    public JavaHtmlSourceRenderer(FileInfo fileInfo, List<LineRenderInfo> lineInfo, HtmlRenderingSupport renderingHelper,
                                  String emptyCoverageMsg, String tab, String space) {
        super(lineInfo, renderingHelper, emptyCoverageMsg, tab, space);
        this.packageInfo = fileInfo.getContainingPackage();
        this.projectInfo = packageInfo.getContainingProject();
    }

    @Override
    public void onImport(String imp) {
        if (imp.endsWith("*")) {
            // wild card import
            String pkg = imp.substring(0, imp.lastIndexOf('.'));
            PackageInfo pkgInfo = projectInfo.getNamedPackage(pkg);
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
                for (PackageInfo pkgInfo : pkgsInScope) {
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
        Map<String, Boolean> lazyMap = LazyMap.decorate(areTestOnlyPackages, () -> {
            for (ClassInfo classInfo : pkg.getAllClasses()) {
                if (!classInfo.isTestClass()) {
                    return Boolean.FALSE;
                }
            }
            return Boolean.TRUE;
        });

        return lazyMap.get(pkg.getName());
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