package org.openclover.eclipse.core.ui.editors.cloud;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.reporters.html.HtmlRenderingSupportImpl;

public class EclipseEditorLinkingHtmlRenderingSupport extends HtmlRenderingSupportImpl {
    private final String currentFile;

    public EclipseEditorLinkingHtmlRenderingSupport(String currentFile) {
        this.currentFile = currentFile;
    }

    @Override
    public StringBuffer getSrcFileLink(boolean toplevel, boolean withAnchor, ClassInfo cls) {
        StringBuffer buf = new StringBuffer(currentFile);
        buf.append(EditorLinkingLocationListener.JAVAEDITOR_HREF_PREFIX);
        buf.append(cls.getQualifiedName());
        return buf;
    }
}
