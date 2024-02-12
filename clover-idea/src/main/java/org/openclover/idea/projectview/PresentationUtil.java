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

        final Icon baseOpenIcon = presentation.getIcon(true);
        if (baseOpenIcon != null) {
            final LayeredIcon openIcon = new LayeredIcon(2);
            openIcon.setIcon(baseOpenIcon, 0);
            openIcon.setIcon(overlay, 1, 0, baseOpenIcon.getIconHeight() - overlay.getIconHeight());
            // TODO: setOpenIcon is deprecated since IDEA13, replace by setIcon()
            presentation.setOpenIcon(openIcon);
        }

        final Icon baseClosedIcon = presentation.getIcon(false);
        if (baseClosedIcon != null) {
            final LayeredIcon closedIcon = new LayeredIcon(2);
            closedIcon.setIcon(baseClosedIcon, 0);
            closedIcon.setIcon(overlay, 1, 0, baseClosedIcon.getIconHeight() - overlay.getIconHeight());
            // TODO: setClosedIcon is deprecated since IDEA13, replace by setIcon()
            presentation.setClosedIcon(closedIcon);
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
