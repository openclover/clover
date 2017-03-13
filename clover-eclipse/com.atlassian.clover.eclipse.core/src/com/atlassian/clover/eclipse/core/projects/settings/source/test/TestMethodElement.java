package com.atlassian.clover.eclipse.core.projects.settings.source.test;

import com.atlassian.clover.spec.instr.test.TestMethodSpec;

import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

public class TestMethodElement implements TreeElement {
    private TestMethodsElement parent;

    private String annotation;
    private String name;
    private String returnType;

    public TestMethodElement(TestMethodsElement parent) {
        this.parent = parent;
    }

    public TestMethodElement(TestMethodsElement parent, String name, String annotation, String returnType) {
        this(parent);
        this.name = name;
        this.annotation = annotation;
        this.returnType = returnType;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public TestMethodSpec toTestMethodSpec() {
        TestMethodSpec testMethodSpec = new TestMethodSpec();
        testMethodSpec.setAnnotation(annotation);
        testMethodSpec.setName(name);
        testMethodSpec.setReturnType(returnType);
        return testMethodSpec;
    }

    @Override
    public TreeElement getParent() {
        return parent;
    }

    public class ReturnTypeNameConditionElement extends SpecificConditionElement {
        public ReturnTypeNameConditionElement(TestMethodElement parent, String value) {
            super(parent, value);
        }
    }

    public List<ConditionElement> getConditions() {
        List<ConditionElement> result = newArrayList();
        if (name != null) {
            result.add(new NameConditionElement(this, name));
        }
        if (annotation != null) {
            result.add(new AnnotationNameConditionElement(this, annotation));
        }
        if (returnType != null) {
            result.add(new ReturnTypeNameConditionElement(this, returnType));
        }
        if (result.isEmpty()) {
            result.add(new AnyConditionElement(this));
        }
        return result;
    }

    public class AnyConditionElement extends com.atlassian.clover.eclipse.core.projects.settings.source.test.AnyConditionElement {
        public AnyConditionElement(TreeElement parent) {
            super(parent);
        }
    }
}
