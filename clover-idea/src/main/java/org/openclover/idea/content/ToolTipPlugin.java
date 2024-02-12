package org.openclover.idea.content;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ElementInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.StatementInfo;
import org.openclover.core.registry.FileElementVisitor;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.feature.CloverFeatures;
import org.openclover.idea.feature.FeatureEvent;
import org.openclover.idea.feature.FeatureListener;
import org.openclover.idea.feature.FeatureManager;

import java.awt.Point;

public class ToolTipPlugin extends ContentPlugin implements EditorMouseMotionListener, FeatureListener {

    private LogicalPosition prevPosition;

    private boolean enabled = false;

    public ToolTipPlugin(Project proj, VirtualFile vf) {
        super(proj, vf);
    }

    @Override
    public void install(Editor e) {
        super.install(e);

        FeatureManager fManager = ProjectPlugin.getPlugin(project).getFeatureManager();
        fManager.addFeatureListener(CloverFeatures.CLOVER_REPORTING_TOOLTIPS, this);
        setEnabled(fManager.isFeatureEnabled(CloverFeatures.CLOVER_REPORTING_TOOLTIPS));

        installedEditor.addEditorMouseMotionListener(this);
    }

    @Override
    public void uninstall() {

        installedEditor.removeEditorMouseMotionListener(this);

        FeatureManager fManager = ProjectPlugin.getPlugin(project).getFeatureManager();
        fManager.removeFeatureListener(CloverFeatures.CLOVER_REPORTING_TOOLTIPS, this);

        super.uninstall();
    }

    @Override
    public void refresh() {
        prevPosition = null;
    }

    //---( Implementation of the EditorMouseMotionListener interface )---

    /**
     * @see EditorMouseMotionListener#mouseMoved(com.intellij.openapi.editor.event.EditorMouseEvent)
     */
    @Override
    public void mouseMoved(EditorMouseEvent evt) {

        if (!enabled || coverageInfo == null) {
            return;
        }

        //TODO: this method overrides existing tooltips, causing problems with
        //TODO: the display (multiple overlapping tooltips) ... should handle
        //TODO: this situation ???

        Point pnt = evt.getMouseEvent().getPoint();

        LogicalPosition pos = installedEditor.xyToLogicalPosition(pnt);
        if (pos.equals(prevPosition)) { // already evaluated this point.
            return;
        }
        prevPosition = pos;

        if (!isCoverageUpToDate()) {
            if (hasPotentialCloverableElements()) {
                installedEditor.getContentComponent().setToolTipText("Coverage data out of date");
            }
            return;
        }

        TooltipProducer tooltipProducer = new TooltipProducer(pos);
        coverageInfo.visitElements(tooltipProducer);

        installedEditor.getContentComponent().setToolTipText(tooltipProducer.toString());

    }

    private static class TooltipProducer implements FileElementVisitor {
        // todo: utilise StatementsAggregatingVisitor when refactoring this class
        private final StringBuilder methodBuilder = new StringBuilder();
        private final StringBuilder branchBuilder = new StringBuilder();
        private final StringBuilder statementBuilder = new StringBuilder();

        private final LogicalPosition position;

        TooltipProducer(LogicalPosition position) {
            this.position = new LogicalPosition(position.line + 1, position.column); //Clover info is 1-based...
        }

        private boolean contains(ElementInfo element) {
            LogicalPosition start = new LogicalPosition(element.getStartLine(), element.getStartColumn());
            LogicalPosition end = new LogicalPosition(element.getEndLine(), element.getEndColumn());

            return position.compareTo(start) >= 0 && position.compareTo(end) < 0;
        }

        @Override
        public void visitClass(ClassInfo info) {
        }

        @Override
        public void visitMethod(MethodInfo info) {
            if (contains(info)) {
                methodBuilder.append("Method <i>").append(info.getSimpleName()).append("</i> invoked ");
                methodBuilder.append(info.getHitCount());
                methodBuilder.append(" times<br>");
            }
        }

        @Override
        public void visitStatement(StatementInfo info) {
            if (contains(info)) {
                statementBuilder.append("Statement ");
                statementBuilder.append(info.getStartLine() + ":" + info.getStartColumn());
                statementBuilder.append(" executed ");
                statementBuilder.append(info.getHitCount());
                statementBuilder.append(" times<br>");
            }
        }

        @Override
        public void visitBranch(BranchInfo info) {
            if (contains(info)) {
                branchBuilder.append("Branch: true ");
                branchBuilder.append(info.getTrueHitCount());
                branchBuilder.append(" times, false ");
                branchBuilder.append(info.getFalseHitCount());
                branchBuilder.append(" times<br>");
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("<html><body>");
            sb.append(methodBuilder);
            sb.append(branchBuilder);
            sb.append(statementBuilder);
            sb.append("</body></html>");
            return sb.toString();
        }
    }

    /**
     * @see EditorMouseMotionListener#mouseDragged(com.intellij.openapi.editor.event.EditorMouseEvent)
     */
    @Override
    public void mouseDragged(EditorMouseEvent evt) {
    }

//---( Implementation of the FeatureListener interface)---

    /**
     * @see FeatureListener#featureStateChanged(org.openclover.idea.feature.FeatureEvent)
     */
    @Override
    public void featureStateChanged(FeatureEvent evt) {
        setEnabled(evt.isEnabled());
    }

    private void setEnabled(boolean b) {
        if (enabled == b) {
            return;
        }
        enabled = b;
        if (!enabled) {
            // do not leave a residual tooltip.
            installedEditor.getContentComponent().setToolTipText(null);
        }
    }
}
