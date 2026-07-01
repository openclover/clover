package org.openclover.idea.projectview;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.psi.PsiElement;
import com.intellij.ui.LayeredIcon;
import org.openclover.idea.IProjectPlugin;
import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.actions.excludes.ExclusionUtil;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.util.ui.CloverIcons;

import javax.swing.Icon;

public class PresentationUtil {
    private PresentationUtil() {
    }

    public static void overlayPresentation(PresentationData presentation, boolean enabled) {
        final Icon overlay = enabled ? CloverIcons.CLOVER_ENABLED_OVL : CloverIcons.CLOVER_DISABLED_OVL;

        final Icon baseIcon = presentation.getIcon(false);
        if (baseIcon != null) {
            final LayeredIcon icon = new LayeredIcon(2);
            icon.setIcon(baseIcon, 0);
            icon.setIcon(overlay, 1, 0, baseIcon.getIconHeight() - overlay.getIconHeight());
            presentation.setIcon(icon);
        }
    }

    public static void overlayPackagePresentation(PresentationData presentation, PsiElement element) {
        final IProjectPlugin plugin = ProjectPlugin.getPlugin(element.getProject());
        if (plugin != null) {
            final IdeaCloverConfig config = plugin.getConfig();
            final String includes = config.getIncludes();
            final String excludes = config.getExcludes();
            final String pattern = ExclusionUtil.getPattern(element);
            final String recursivePattern = ExclusionUtil.getRecursivePattern(element);
            if (ExclusionUtil.isExplicitlyIncluded(includes, pattern) || ExclusionUtil.isExplicitlyIncluded(includes, recursivePattern)) {
                overlayPresentation(presentation, true);
            } else if(ExclusionUtil.isExplicitlyIncluded(excludes, pattern) || ExclusionUtil.isExplicitlyIncluded(excludes, recursivePattern)) {
                overlayPresentation(presentation, false);
            }
        }
    }
}
