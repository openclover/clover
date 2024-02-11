package org.openclover.idea.content;

import com.atlassian.clover.CloverDatabase;
import org.openclover.runtime.Logger;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.util.vfs.VfsUtil;
import org.openclover.idea.coverage.CoverageTreeListener;
import org.openclover.idea.coverage.CoverageTreeModel;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import javax.swing.SwingUtilities;

public abstract class ContentPlugin implements DocumentListener, CoverageTreeListener {

    protected static final Logger LOG = Logger.getInstance(ContentPlugin.class.getName());

    private final String openFilename;
    protected final Project project;

    protected CoverageTreeModel currentCoverageModel;
    protected FullFileInfo coverageInfo;
    protected Editor installedEditor;
    private final PsiFile psiFile;

    public ContentPlugin(Project proj, VirtualFile vf) {
        project = proj;
        openFilename = VfsUtil.getRootRelativeFilename(proj, vf);
        psiFile = PsiManager.getInstance(project).findFile(vf);
    }

    public ContentManager getContentManager() {
        return ((ProjectPlugin) ProjectPlugin.getPlugin(project)).getContentManager();
    }

    /**
     * @param editor editor to install this plugin on.
     */
    public void install(Editor editor) {
        installedEditor = editor;
        IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(project);
        projectPlugin.getCoverageManager().addCoverageTreeListener(this);

        update(projectPlugin.getCoverageManager().getCoverageTree());

        installedEditor.getDocument().addDocumentListener(this);
    }

    /**
     *
     */
    public void uninstall() {
        installedEditor.getDocument().removeDocumentListener(this);

        IProjectPlugin projectPlugin = ProjectPlugin.getPlugin(project);
        projectPlugin.getCoverageManager().removeCoverageTreeListener(this);

        installedEditor = null;
    }

    protected final boolean isDocumentSaved() {
        return !FileDocumentManager.getInstance().isDocumentUnsaved(installedEditor.getDocument());
    }

    protected boolean isCoverageUpToDate() {
        return coverageInfo != null && isDocumentSaved() && coverageInfo.validatePhysicalFile();
    }

    //---( Implementation of the DocumentListener interface )---

    @Override
    @SuppressWarnings({"NoopMethodInAbstractClass"})
    public void beforeDocumentChange(DocumentEvent evt) {
    }

    @Override
    public void documentChanged(DocumentEvent evt) {
        refresh();
    }

    //---( Implementation of the CoverageListener interface )---

    @Override
    public void update(CoverageTreeModel coverageModel) {
        currentCoverageModel = coverageModel;
        final CloverDatabase db = coverageModel != null ? coverageModel.getCloverDatabase() : null;
        coverageInfo = db != null ? (FullFileInfo) db.getFullModel().findFile(openFilename) : null;
        SwingUtilities.invokeLater(this::refresh);
    }

    protected boolean hasPotentialCloverableElements() {
        if (coverageInfo != null && coverageInfo.getDataLength() != 0) {
            return true;
        }

        for (PsiElement psiElement : psiFile.getChildren()) {
            if (psiElement instanceof PsiClass) {
                if (hasPotentialCloverableElements((PsiClass) psiElement)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasPotentialCloverableElements(PsiClass psiClass) {
        if (!psiClass.isInterface()) {
            if (psiClass.getMethods().length != 0 || psiClass.getInitializers().length != 0) {
                // constructors would are returned also as methods
                return true;
            }
        }

        for (PsiClass aClass : psiClass.getInnerClasses()) {
            if (hasPotentialCloverableElements(aClass)) {
                return true;
            }
        }

        return false;
    }

    public abstract void refresh();
}
