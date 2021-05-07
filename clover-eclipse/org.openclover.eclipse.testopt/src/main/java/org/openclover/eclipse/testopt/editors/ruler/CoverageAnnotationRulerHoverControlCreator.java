package org.openclover.eclipse.testopt.editors.ruler;

import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlCreatorExtension;
import org.eclipse.swt.widgets.Shell;

public class CoverageAnnotationRulerHoverControlCreator implements
        IInformationControlCreator, IInformationControlCreatorExtension {

    @Override
    public IInformationControl createInformationControl(Shell parent) {
        return new CoverageAnnotationRulerInformationControl(parent);
    }

    @Override
    public boolean canReplace(IInformationControlCreator creator) {
        return creator.getClass() == CoverageAnnotationRulerHoverControlCreator.class;
    }

    @Override
    public boolean canReuse(IInformationControl control) {
        return control instanceof CoverageAnnotationRulerInformationControl;
    }

}
