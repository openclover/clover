package org.openclover.idea.report.cloud;


import org.openclover.idea.util.psi.PsiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class ClassHyperlinkListener implements HyperlinkListener {
    private final Project project;

    public ClassHyperlinkListener(Project project) {
        this.project = project;
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            focusClass(e.getDescription());
        }
    }

    private void focusClass(String className) {
        final PsiClass[] matchingClasses = PsiUtil.findClasses(className, project);
        for (PsiClass aClass : matchingClasses) {
            if (aClass.canNavigate()) {
                aClass.navigate(true);
                break;
            }
        }
    }
}
