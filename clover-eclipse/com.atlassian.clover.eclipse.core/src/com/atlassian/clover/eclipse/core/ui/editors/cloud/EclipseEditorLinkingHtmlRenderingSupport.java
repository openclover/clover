package com.atlassian.clover.eclipse.core.ui.editors.cloud;

import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.reporters.html.HtmlRenderingSupportImpl;

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
