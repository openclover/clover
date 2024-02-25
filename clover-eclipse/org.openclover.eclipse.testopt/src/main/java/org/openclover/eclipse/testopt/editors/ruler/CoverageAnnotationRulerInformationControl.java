package org.openclover.eclipse.testopt.editors.ruler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.editors.text.EditorsUI;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.eclipse.core.ui.editors.java.CoverageAnnotation;
import org.openclover.eclipse.core.ui.editors.java.CoverageAnnotation.Kind;
import org.openclover.eclipse.core.ui.editors.java.ILineCoverageModel.Entry;
import org.openclover.eclipse.testopt.editors.ruler.tree.CoverageAnnotationContentProvider;
import org.openclover.eclipse.testopt.editors.ruler.tree.TestNameLabelProvider;

public class CoverageAnnotationRulerInformationControl extends
        PopupDialog implements IInformationControl,
        IInformationControlExtension, IInformationControlExtension2 {

    private TreeViewer treeViewer;
    private CoverageAnnotationRulerHover.CoverageAnnotationInput lastInput;
    
    public CoverageAnnotationRulerInformationControl(Shell parentShell) {
        super(parentShell, SWT.ON_TOP | SWT.RESIZE, false, true, true, true, false, "", "");
        create();
    }

    @Override
    public void setInput(Object input) {
        lastInput = input instanceof CoverageAnnotationRulerHover.CoverageAnnotationInput ? (CoverageAnnotationRulerHover.CoverageAnnotationInput)input : null;
        treeViewer.setInput(input);
        if (lastInput != null) {
            final String message = getText(lastInput.annotation) 
            + "\nCovered by " + lastInput.testCases.size() + " test runs";
            setTitleText(message);
        } else {
            setTitleText("");
        }
     
        setInfoText(EditorsUI.getTooltipAffordanceString());
    }

    private String getText(Entry annotation) {
        final Kind kind = Kind.kindFor(false, annotation.getElementInfo(), annotation.getTestCaseInfos());
        return CoverageAnnotation.textForCoverage(annotation.getElementInfo(), kind, annotation.getTestCaseInfos());
    }

    @Override
    public boolean hasContents() {
        return lastInput != null;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite mainComposite = new Composite(parent, SWT.NONE);
        mainComposite.setLayout(new GridLayout());
        
        treeViewer = new TreeViewer(parent, SWT.SINGLE | SWT.V_SCROLL);
        GridDataFactory.fillDefaults().grab(true, true).hint(500, 200).applyTo(treeViewer.getControl());
        treeViewer.setContentProvider(new CoverageAnnotationContentProvider());
        final TreeViewerColumn nameColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
        nameColumn.getColumn().setText("Test Cases");
        nameColumn.getColumn().setResizable(true);
        nameColumn.setLabelProvider(new TestNameLabelProvider());
        
        treeViewer.addDoubleClickListener(event -> {
            Object o = ((StructuredSelection)event.getSelection()).getFirstElement();
            if (o instanceof TestCaseInfo) {
                TestCaseInfo tci = (TestCaseInfo) o;
                final String pattern = tci.getSourceMethodName();
                SearchPattern searchPattern = SearchPattern.createPattern(pattern, IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH|SearchPattern.R_CASE_SENSITIVE);
                IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
                SearchRequestor requestor = new SearchRequestor() {

                    @Override
                    public void acceptSearchMatch(SearchMatch match) throws CoreException {
                        final Object element = match.getElement();
                        if (element instanceof IJavaElement) {
                            JavaUI.openInEditor((IJavaElement) element);
                        }
                    }
                };
                SearchEngine searchEngine = new SearchEngine();
                try {
                    searchEngine.search(searchPattern, new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, scope, requestor, null);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            } else {
                final TreePath[] paths = ((ITreeSelection)event.getViewer().getSelection()).getPaths();
                if (paths.length > 0) {
                    if (treeViewer.getExpandedState(paths[0])) {
                        treeViewer.collapseToLevel(paths[0], TreeViewer.ALL_LEVELS);
                    } else {
                        treeViewer.expandToLevel(paths[0], 1);
                    }
                }
            }

        });
        treeViewer.getControl().addMouseMoveListener(mouseEvent -> {
            final Tree tree = treeViewer.getTree();
            final TreeItem item = tree.getItem(new Point(mouseEvent.x, mouseEvent.y));
            if (item != null && item.getData() instanceof TestCaseInfo) {
                if (tree.getCursor() == null) {
                    tree.setCursor(tree.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
                }
            } else {
                tree.setCursor(null);
            }
        });
        
        treeViewer.setComparator(new ViewerComparator());
        treeViewer.getControl().addControlListener(new ControlListener() {
            @Override
            public void controlResized(ControlEvent e) {
               final Rectangle clientArea = treeViewer.getTree().getClientArea();
               nameColumn.getColumn().setWidth(clientArea.width);
            }
            
            @Override
            public void controlMoved(ControlEvent e) {
            }
        });

        return mainComposite;
    }
    
    @Override
    public void addDisposeListener(DisposeListener listener) {
        getShell().addDisposeListener(listener);
    }

    @Override
    public void addFocusListener(FocusListener listener) {
        getShell().addFocusListener(listener);
    }

    @Override
    public Point computeSizeHint() {
        return getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
    }

    @Override
    public void dispose() {
        close();
    }

    @Override
    public boolean isFocusControl() {
        return getShell().getDisplay().getActiveShell() == getShell();
    }

    @Override
    public void removeDisposeListener(DisposeListener listener) {
        getShell().removeDisposeListener(listener);
    }

    @Override
    public void removeFocusListener(FocusListener listener) {
        getShell().removeFocusListener(listener);
    }

    @Override
    public void setBackgroundColor(Color background) {
        applyBackgroundColor(background, getContents());
    }

    @Override
    public void setFocus() {
        getShell().forceFocus();
        setInfoText("Double-click the test name to open test source");
    }

    @Override
    public void setForegroundColor(Color foreground) {
        applyForegroundColor(foreground, getContents());
    }

    @Override
    public void setInformation(String information) {
    }

    @Override
    public void setLocation(Point location) {
        getShell().setLocation(location);
    }

    @Override
    public void setSize(int width, int height) {
        getShell().setSize(width, height);
    }

    @Override
    public void setSizeConstraints(int maxWidth, int maxHeight) {
        // ignore
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            open();
        } else {
            saveDialogBounds(getShell());
            getShell().setVisible(false);
        }
    }
    
    
}
