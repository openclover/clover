package org.openclover.eclipse.core.reports;

import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.reports.model.ReportHistoryEntry;
import org.openclover.core.reporters.Type;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;
import org.eclipse.jdt.internal.debug.ui.StorageEditorInput;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.ide.IDE;

import java.io.File;
import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Lists.newLinkedList;

public abstract class OpenReportOperation {

    public static abstract class OpenReportWithSystemEditor extends OpenReportOperation {
        @Override
        public void open(ReportHistoryEntry entry) {
            try {
                IDE.openEditor(getActivePage(), inputFor(entry.getPath()), IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
            } catch (Exception e) {
                CloverPlugin.logError("Unable to launch system reader for " + entry.getPath(), e);
            }
        }
    }

    public static class OpenWithEditor extends OpenReportOperation {
        private String editorId;
        private Type type;
        private final String name;

        protected OpenWithEditor(String editorId, Type type, String name) {
            this.editorId = editorId;
            this.type = type;
            this.name = name;
        }

        @Override
        public String getName() {
            return CloverEclipsePluginMessages.ECLIPSE_VIEWER(name);
        }

        @Override
        public boolean supports(ReportHistoryEntry report) {
            return type == report.getType();
        }

        @Override
        public void open(ReportHistoryEntry entry) {
            try {
                IDE.openEditor(getActivePage(), inputFor(entry.getPath()), editorId);
            } catch (Exception e) {
                CloverPlugin.logError("Unable to launch Eclipse editor " + editorId, e);
            }
        }
    };

    public static OpenReportOperation OPEN_PDF = new OpenReportWithSystemEditor() {
        @Override
        public String getName() {
            return CloverEclipsePluginMessages.SYSTEM_PDF_VIEWER();
        }

        @Override
        public boolean supports(ReportHistoryEntry report) {
            return report.getType() == Type.PDF;
        }
    };

    public static OpenReportOperation OPEN_XML_WITH_SYSTEM_EDITOR = new OpenReportWithSystemEditor() {
        @Override
        public String getName() {
            return CloverEclipsePluginMessages.SYSTEM_XML_VIEWER();
        }

        @Override
        public boolean supports(ReportHistoryEntry report) {
            return report.getType() == Type.XML;
        }
    };

    public static OpenReportOperation OPEN_HTML_WITH_SYSTEM_BROWSER = new OpenReportOperation() {
        @Override
        public String getName() {
            return CloverEclipsePluginMessages.SYSTEM_BROWSER();
        }

        @Override
        public boolean supports(ReportHistoryEntry report) {
            return
                report.getType() == Type.HTML
                && PlatformUI.getWorkbench().getBrowserSupport().isInternalWebBrowserAvailable();
        }

        @Override
        public void open(ReportHistoryEntry entry) {
            try {
                IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
                IWebBrowser browser = browserSupport.getExternalBrowser();
                if (browser != null) {
                    browser.openURL(new File(entry.getPath()).toURI().toURL());
                } else {
                    CloverPlugin.logWarning("No system HTML browser");
                }
            } catch (Exception e) {
                CloverPlugin.logError("Unable to launch system HTML browser", e);
            }
        }
    };

    public static OpenReportOperation OPEN_HTML_WITH_ECLIPSE_BROWSER = new OpenReportOperation() {
        @Override
        public String getName() {
            return CloverEclipsePluginMessages.ECLIPSE_BROWSER();
        }

        @Override
        public boolean supports(ReportHistoryEntry report) {
            return report.getType() == Type.HTML;
        }

        @Override
        public void open(ReportHistoryEntry entry) {
            try {
                IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
                IWebBrowser browser = browserSupport.createBrowser("Clover HTML Report Browser");
                if (browser != null) {
                    browser.openURL(new File(entry.getPath()).toURI().toURL());
                } else {
                    CloverPlugin.logWarning("No Eclipse HTML browser");
                }
            } catch (Exception e) {
                CloverPlugin.logError("Unable to open Eclipse HTML browser", e);
            }
        }
    };

    private static final OpenReportOperation[] OPERATIONS;

    /**
     * The org.eclipse.ui.internal.Workbench class implements
     *  - org.eclipse.ui.IWorkbench in Eclipse 3.6-4.3
     *  - org.eclipse.ui.IWorkbench + org.eclipse.e4.ui.workbench.IWorkbench in Eclipse 4.4
     * As we compile Clover against Eclipse 3.6, it fails with class initialization error in Eclipse 4.4.
     *
     * Workaround: a Workbench class is instantiated using reflections to avoid any bytecode dependencies in our code.
     */
    private static IEditorRegistry getEditorRegistryFromWorkbench() {
        // call: Workbench.getInstance().getEditorRegistry()
        try {
            final Class workbenchClass = Class.forName("org.eclipse.ui.internal.Workbench");
            final Object workbenchInstance  = workbenchClass.getMethod("getInstance").invoke(null);
            final Object editorRegistry = workbenchClass.getMethod("getEditorRegistry").invoke(workbenchInstance);
            return (IEditorRegistry)editorRegistry;
        } catch (Exception e) {
            CloverPlugin.logError("Failed to retrieve editor registry", e);
        }
        return null;
    }

    static {
        List<OpenReportOperation> operations = newArrayList(
                OPEN_PDF,
                OPEN_XML_WITH_SYSTEM_EDITOR,
                OPEN_HTML_WITH_SYSTEM_BROWSER,
                OPEN_HTML_WITH_ECLIPSE_BROWSER);

        //Check installd editors for xml files
        IEditorDescriptor[] xmlEditors = getEditorRegistryFromWorkbench().getEditors(
            "foobar.xml",
            Platform.getContentTypeManager().getContentType("org.eclipse.core.runtime.xml"));
        for (IEditorDescriptor xmlEditor : xmlEditors) {
            if (xmlEditor.isInternal()) {
                operations.add(
                        new OpenWithEditor(xmlEditor.getId(), Type.XML, xmlEditor.getLabel()));
            }
        }

        OPERATIONS = operations.toArray(new OpenReportOperation[operations.size()]);
    }

    private static IEditorDescriptor[] xmlEditorsFor(ReportHistoryEntry report) {
        return getEditorRegistryFromWorkbench().getEditors(
                report.getPath(),
                Platform.getContentTypeManager().getContentType("org.eclipse.core.runtime.xml"));
    }

    public static List findFor(ReportHistoryEntry entry) {
        List supportedMethods = newLinkedList();
        for (OpenReportOperation reportOperation : OPERATIONS) {
            if (reportOperation.supports(entry)) {
                supportedMethods.add(reportOperation);
            }
        }
        return supportedMethods;
    }

    public abstract String getName();

    public abstract boolean supports(ReportHistoryEntry report);

    public abstract void open(ReportHistoryEntry entry);

    protected IWorkbenchPage getActivePage() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    }

    protected IEditorInput inputFor(String path) {
        return new MyEditorInput(new File(path));
    }

    private static class MyEditorInput extends StorageEditorInput implements IPathEditorInput {
        private final File file;

        public MyEditorInput(File file) {
            super(new LocalFileStorage(file));
            this.file = file;
        }

        @Override
        public boolean exists() {
            return file.exists();
        }

        @Override
        public IPath getPath() {
            return new Path(file.getPath());
        }
    }
}
