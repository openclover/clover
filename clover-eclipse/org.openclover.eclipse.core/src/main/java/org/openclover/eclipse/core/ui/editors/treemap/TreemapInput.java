package org.openclover.eclipse.core.ui.editors.treemap;

import org.openclover.eclipse.core.ui.editors.CloverProjectInput;
import org.openclover.eclipse.core.projects.CloverProject;

/**
 * We subclass CloverProjectInput in this trivial fashion to
 * ensure treemap project inputs and cloud project inputs
 * are never equal. This is used by Stupiclipse to
 * worked out if an editor should be shown (it's not
 * if there's already one of any type showing with an
 * equivalent input)
 */
public class TreemapInput extends CloverProjectInput {
    public TreemapInput(CloverProject project) {
        super(project);
    }
}
