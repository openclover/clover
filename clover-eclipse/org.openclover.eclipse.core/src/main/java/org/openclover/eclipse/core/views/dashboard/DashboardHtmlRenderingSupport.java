package org.openclover.eclipse.core.views.dashboard;

import java.net.URI;
import java.net.URISyntaxException;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.core.reporters.html.HtmlRenderingSupportImpl;

public class DashboardHtmlRenderingSupport extends HtmlRenderingSupportImpl {
    public static final String JAVA_METHOD_SCHEME = "javamethod";
    public static final String JAVA_CLASS_SCHEME = "javaclass";
    public static final String JAVA_PACKAGE_SCHEME = "javapkg";

    @Override
    public StringBuffer getSrcFileLink(boolean toplevel, boolean withAnchor, ClassInfo cls) {
        return new StringBuffer(makeURI(JAVA_CLASS_SCHEME, cls.getContainingFile().getPackagePath(),
                cls.getStartLine(), cls.getStartColumn()));
    }

    @Override
    public String getPkgURLPath(String aPkg) {
        return JAVA_PACKAGE_SCHEME + ':' + aPkg;
    }

    @Override
    public String getMethodLink(boolean toplevel, FullMethodInfo mthd) {
        return makeURI(JAVA_METHOD_SCHEME, mthd.getContainingFile().getPackagePath(),
                mthd.getStartLine(), mthd.getStartColumn());
    }

    private static String makeURI(String scheme, String path, int line, int column) {
        try {
            final URI uri = new URI(scheme, null, "/" + path, String.valueOf(line) + '_' + column);
            return uri.toString();
        } catch (final URISyntaxException e) {
            CloverPlugin.logError("Cannot create URI", e);
            return "";
        }
    }

    public String formatMethodName(String methodName) {
        final String escapedName = htmlEscapeStr(methodName);
        return escapedName.replaceAll("(,|\\(|\\))", "$1<wbr>");
    }
}
