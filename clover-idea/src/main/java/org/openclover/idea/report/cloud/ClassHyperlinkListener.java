package org.openclover.idea.report.cloud;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import org.openclover.idea.util.psi.PsiUtil;

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
        final PsiClass classToNavigate = ApplicationManager.getApplication().runReadAction((Computable<PsiClass>) () -> {
            for (PsiClass aClass : PsiUtil.findClasses(className, project)) {
                if (aClass.canNavigate()) {
                    return aClass;
                }
            }
            return null;
        });
        if (classToNavigate != null) {
            classToNavigate.navigate(true);
        }
    }
}
