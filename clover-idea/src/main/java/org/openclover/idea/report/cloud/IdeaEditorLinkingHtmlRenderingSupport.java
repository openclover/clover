package org.openclover.idea.report.cloud;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.reporters.html.HtmlRenderingSupportImpl;

public class IdeaEditorLinkingHtmlRenderingSupport extends HtmlRenderingSupportImpl {

    @Override
    public StringBuffer getSrcFileLink(boolean toplevel, boolean withAnchor, ClassInfo cls) {
        return new StringBuffer(cls.getQualifiedName());
    }
}
