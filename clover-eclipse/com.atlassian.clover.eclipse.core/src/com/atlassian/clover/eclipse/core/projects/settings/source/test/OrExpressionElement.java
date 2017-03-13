package com.atlassian.clover.eclipse.core.projects.settings.source.test;

import com.atlassian.clover.spec.instr.test.BooleanSpec;
import com.atlassian.clover.spec.instr.test.OrSpec;

public class OrExpressionElement extends ExpressionElement {
    public OrExpressionElement(TreeElement parent) {
        super(parent);
    }

    @Override
    public BooleanSpec newSpec() {
        return new OrSpec();
    }
}
