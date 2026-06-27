package org.openclover.eclipse.core.reports;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.PlatformUI;
import org.openclover.core.reporters.Type;
import org.openclover.eclipse.core.reports.model.ReportHistoryEntry;

import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Lists.newLinkedList;

public class OpenReportOperations {

    public static final OpenReportOperation OPEN_PDF = new OpenPdfWithSystemEditor();
    public static final OpenReportOperation OPEN_XML_WITH_SYSTEM_EDITOR = new OpenXmlWithSystemEditor();
    public static final OpenReportOperation OPEN_HTML_WITH_SYSTEM_BROWSER = new OpenHtmlWithSystemBrowser();
    public static final OpenReportOperation OPEN_HTML_WITH_ECLIPSE_BROWSER = new OpenHtmlWithEclipseBrowser();

    private static final OpenReportOperation[] OPERATIONS;

    static {
        List<OpenReportOperation> operations = newArrayList(
                OPEN_PDF,
                OPEN_XML_WITH_SYSTEM_EDITOR,
                OPEN_HTML_WITH_SYSTEM_BROWSER,
                OPEN_HTML_WITH_ECLIPSE_BROWSER);

        IEditorDescriptor[] xmlEditors = PlatformUI.getWorkbench().getEditorRegistry().getEditors(
                "foobar.xml",
                Platform.getContentTypeManager().getContentType("org.eclipse.core.runtime.xml"));
        for (IEditorDescriptor xmlEditor : xmlEditors) {
            if (xmlEditor.isInternal()) {
                operations.add(new OpenWithEditor(xmlEditor.getId(), Type.XML, xmlEditor.getLabel()));
            }
        }

        OPERATIONS = operations.toArray(new OpenReportOperation[0]);
    }

    public static List findFor(ReportHistoryEntry entry) {
        List supportedMethods = newLinkedList();
        for (OpenReportOperation operation : OPERATIONS) {
            if (operation.supports(entry)) {
                supportedMethods.add(operation);
            }
        }
        return supportedMethods;
    }
}
