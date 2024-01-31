package org.openclover.eclipse.core.ui.editors.treemap;

import org.openclover.eclipse.core.CloverEclipsePluginMessages;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.ui.CloverPluginIcons;
import org.openclover.eclipse.core.ui.GLH;
import org.openclover.eclipse.core.ui.editors.CloverProjectInput;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.jdt.core.IJavaProject;
import net.sf.jtreemap.ktreemap.KTreeMap;
import net.sf.jtreemap.ktreemap.TreeMapNode;
import net.sf.jtreemap.ktreemap.SplitSquarified;
import net.sf.jtreemap.ktreemap.ITreeMapProvider;

public class TreemapEditor extends EditorPart {
    public static final String ID = CloverPlugin.ID + ".editors.treemap";

    private DrillDownAdapter drillDownAdapter;
    private Action openXmlAction;
    private Action openTM3Action;
    private Action selectionChangedAction;
    private KTreeMap treeMap;
    private Composite legend;
    private CloverTreeMapProvider cloverProvider;

    private Button refreshButton;

    @Override
    public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
        if (!(editorInput instanceof TreemapInput)) {
            throw new PartInitException("Invalid Input: must be TreemapInput");
        } else {
            setSite(site);
            setInput(editorInput);
        }
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void createPartControl(Composite composite) {

        cloverProvider = new CloverTreeMapProvider();

        TreeMapNode root = getRoot((CloverProjectInput)getEditorInput());

        composite.setLayout(new GLH().numColumns(1).getGridLayout());

        createKTreeMapComp(composite, root);
        createRefreshButton(composite);

        hookContextMenu();
    }

    private void createRefreshButton(Composite parent) {
        Composite row = new Composite(parent, SWT.NONE);

        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        row.setLayoutData(gridData);

        row.setLayout(new GridLayout());

        refreshButton = new Button(row, SWT.NONE);
        refreshButton.setImage(CloverPlugin.getImage(CloverPluginIcons.PROJECT_REFRESH_ICON));
        refreshButton.setText(CloverEclipsePluginMessages.REFRESH_CLOUD_BUTTON());
        refreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                Display.getDefault().asyncExec(() -> {
                    treeMap.setRoot(getRoot((CloverProjectInput)getEditorInput()));
                    treeMap.redraw();
                });
            }
        });
    }

    private TreeMapNode getRoot(CloverProjectInput input) {
        return new ProjectHeatMapBuilder().buildTree(input.getProject());
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        throw new UnsupportedOperationException("Save not permitted");
    }

    @Override
    public void doSaveAs() {
        throw new UnsupportedOperationException("Save not permitted");
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
    }

    private void createKTreeMapComp(Composite parent, TreeMapNode root) {
        treeMap = new CloverTreeMap(getJavaProject(), getSite(), parent, SWT.NONE, root);
        treeMap.setTreeMapProvider(cloverProvider);
        treeMap.setColorProvider(new EclipseCoverageColorProvider(cloverProvider));
        treeMap.setStrategy(new SplitSquarified());

        GridData gridData = new GridData(GridData.FILL_BOTH);
        treeMap.setLayoutData(gridData);
    }

    private IJavaProject getJavaProject() {
        try {
            return ((CloverProjectInput)getEditorInput()).getProject().getJavaProject();
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to retrieve Java project for treemap editor", e);
            return null;
        }
    }

    private void fillContextMenu(IMenuManager manager) {
        TreeMapNode orig = treeMap.getDisplayedRoot();

        ITreeMapProvider provider = treeMap.getTreeMapProvider();

        TreeMapNode cursor = orig;

        // Separator
        String id = "separator";
        manager.add(new Separator(id));

        // Parents
        while (cursor.getParent() != null) {
            TreeMapNode parent = cursor.getParent();
            ZoomAction action =
                new ZoomAction(
                    provider.getLabel(parent),
                    CloverPlugin.getImageDescriptor(CloverPluginIcons.ZOOMOUT), parent);
            manager.insertBefore(id, action);
            cursor = parent;
            id = action.getId();
        }

        // children
        cursor = orig;
        while (cursor.getChild(treeMap.getCursorPosition()) != null) {
            TreeMapNode child = cursor.getChild(treeMap.getCursorPosition());
            if (!child.isLeaf()) {
                ZoomAction action =
                    new ZoomAction(
                        provider.getLabel(child),
                        CloverPlugin.getImageDescriptor(CloverPluginIcons.ZOOMIN), child);
                manager.add(action);
            }
            cursor = child;
        }
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        drillDownAdapter.addNavigationActions(manager);
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> fillContextMenu(manager));
        Menu menu = menuMgr.createContextMenu(treeMap);
        treeMap.setMenu(menu);
    }

    private class ZoomAction extends Action {
        private TreeMapNode node;

        /**
         * Constructor
         *
         * @param text  text of the action
         * @param image image
         * @param node  destination TreeMapNode of the zoom
         */
        public ZoomAction(String text, ImageDescriptor image, TreeMapNode node) {
            super(text, image);
            this.node = node;
            setId(text);
        }

        /*
               * (non-Javadoc)
               *
               * @see javax.swing.Action#isEnabled()
               */
        @Override
        public boolean isEnabled() {
            return true;
        }

        /*
               * (non-Javadoc)
               *
               * @see org.eclipse.jface.action.Action#run()
               */
        @Override
        public void run() {
            treeMap.zoom(this.node);
            treeMap.redraw();
        }
    }

}
