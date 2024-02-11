package org.openclover.eclipse.core.projects.settings.source.test;

import org.openclover.core.spec.instr.test.BooleanSpec;
import org.openclover.core.spec.instr.test.OrSpec;

public class OrExpressionElement extends ExpressionElement {
    public OrExpressionElement(TreeElement parent) {
        super(parent);
    }

    @Override
    public BooleanSpec newSpec() {
        return new OrSpec();
    }
}
