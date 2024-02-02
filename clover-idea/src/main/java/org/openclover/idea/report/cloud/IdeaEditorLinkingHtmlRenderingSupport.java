package com.atlassian.clover.idea.report.cloud;

import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.reporters.html.HtmlRenderingSupportImpl;

public class IdeaEditorLinkingHtmlRenderingSupport extends HtmlRenderingSupportImpl {

    @Override
    public StringBuffer getSrcFileLink(boolean toplevel, boolean withAnchor, ClassInfo cls) {
        return new StringBuffer(cls.getQualifiedName());
    }
}
