package org.openclover.eclipse.core.reports;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.openclover.core.reporters.Type;
import org.openclover.eclipse.core.reports.model.ReportHistoryEntry;

import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Lists.newLinkedList;
import static org.openclover.eclipse.core.CloverPlugin.logError;

public class OpenReportOperations {

    public static final OpenReportOperation OPEN_PDF = new OpenPdfWithSystemEditor();
    public static final OpenReportOperation OPEN_XML_WITH_SYSTEM_EDITOR = new OpenXmlWithSystemEditor();
    public static final OpenReportOperation OPEN_HTML_WITH_SYSTEM_BROWSER = new OpenHtmlWithSystemBrowser();
    public static final OpenReportOperation OPEN_HTML_WITH_ECLIPSE_BROWSER = new OpenHtmlWithEclipseBrowser();

    private static final OpenReportOperation[] OPERATIONS;

    /**
     * The org.eclipse.ui.internal.Workbench class implements
     *  - org.eclipse.ui.IWorkbench in Eclipse 3.6-4.3
     *  - org.eclipse.ui.IWorkbench + org.eclipse.e4.ui.workbench.IWorkbench in Eclipse 4.4
     * As we compile Clover against Eclipse 3.6, it fails with class initialization error in Eclipse 4.4.
     * Workaround: a Workbench class is instantiated using reflections to avoid any bytecode dependencies in our code.
     */
    private static IEditorRegistry getEditorRegistryFromWorkbench() {
        try {
            final Class<?> workbenchClass = Class.forName("org.eclipse.ui.internal.Workbench");
            final Object workbenchInstance = workbenchClass.getMethod("getInstance").invoke(null);
            final Object editorRegistry = workbenchClass.getMethod("getEditorRegistry").invoke(workbenchInstance);
            return (IEditorRegistry) editorRegistry;
        } catch (Exception e) {
            logError("Failed to retrieve editor registry", e);
        }
        return null;
    }

    static {
        List<OpenReportOperation> operations = newArrayList(
                OPEN_PDF,
                OPEN_XML_WITH_SYSTEM_EDITOR,
                OPEN_HTML_WITH_SYSTEM_BROWSER,
                OPEN_HTML_WITH_ECLIPSE_BROWSER);

        IEditorDescriptor[] xmlEditors = getEditorRegistryFromWorkbench().getEditors(
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
