package org.openclover.eclipse.core.ui.editors.java.actions;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.openclover.eclipse.core.CloverPlugin;

public class QuickShowTestContributionsAction implements IEditorActionDelegate {
    private IEditorPart editorPart;
    private static final String FILTER_COMMAND_ID = CloverPlugin.ID + "." + "editors.java.QuickCoverageFilter";

    @Override
    public void setActiveEditor(IAction action, IEditorPart editorPart) {
        this.editorPart = editorPart;
    }

    @Override
    public void run(IAction action) {
        if (editorPart instanceof CompilationUnitEditor) {

//          class QuickCoverageFilter {
//              QuickCoverageFilter(Shell parent) {
//                  final Shell shell = new Shell(SWT.TOOL);
//                  shell.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
//                  shell.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
//                  shell.addFocusListener(new FocusListener() {
//                      public void focusLost(FocusEvent focusEvent) {
//                          shell.close();
//                      }
//
//                      public void focusGained(FocusEvent focusEvent) {
//
//                      }
//                  });
//                  shell.setLayout(new FillLayout());
//
//                  TestContributionsTree tree =
//                      new TestContributionsTree(
//                          shell,
//                          SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL,
//                          shell.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND),
//                          shell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
//
//                  tree.setInput(editorPart);
//                  shell.pack();
//
//                  shell.setVisible(true);
//              }
//          }
//
            QuickCoverageFilter filter = new QuickCoverageFilter(
                editorPart.getSite().getShell(),
                SWT.NONE,
                SWT.NONE,
                FILTER_COMMAND_ID, false);
            filter.setInput(editorPart);
            filter.setVisible(true);
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {

    }
}

