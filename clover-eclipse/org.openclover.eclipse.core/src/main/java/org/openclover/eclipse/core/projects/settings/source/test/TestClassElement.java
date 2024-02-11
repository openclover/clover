package org.openclover.eclipse.core.projects.settings.source.test;

import org.openclover.core.spec.instr.test.TestClassSpec;

import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;

public class TestClassElement implements TreeElement {
    private ExpressionElement parent;

    private String pkg;
    private String annotation;
    private String name;
    private String superName;
    private TestMethodsElement testMethods = new TestMethodsElement(this);

    public TestClassElement(ExpressionElement parent) {
        this.parent = parent;
    }

    public String getPackage() {
        return pkg;
    }

    public String getAnnotation() {
        return annotation;
    }

    public String getName() {
        return name;
    }

    public TestMethodsElement getTestMethods() {
        return testMethods;
    }

    public String getSuper() {
        return superName;
    }

    public void setPackage(String pkg) {
        this.pkg = pkg;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSuper(String superName) {
        this.superName = superName;
    }

    public TestClassSpec toTestClassSpec() {
        TestClassSpec result = new TestClassSpec();
        result.setPackage(pkg);
        result.setName(name);
        result.setAnnotation(annotation);
        result.setSuper(superName);

        for (int i = 0; i < testMethods.getMethods().size(); i++) {
            TestMethodElement testMethodElement = testMethods.getMethods().get(i);
            result.addConfiguredTestMethod(testMethodElement.toTestMethodSpec());
        }
        
        return result;
    }

    @Override
    public TreeElement getParent() {
        return parent;
    }

    public class PackageNameConditionElement extends SpecificConditionElement {
        public PackageNameConditionElement(TestClassElement parent, String value) {
            super(parent, value);
        }
    }

    public class SuperTypeNameConditionElement extends SpecificConditionElement {
        public SuperTypeNameConditionElement(TestClassElement parent, String value) {
            super(parent, value);
        }
    }

    public List<ConditionElement> getConditions() {
        List<ConditionElement> result = newArrayList();
        if (name != null) {
            result.add(new NameConditionElement(this, name));
        }
        if (pkg != null) {
            result.add(new PackageNameConditionElement(this, pkg));
        }
        if (annotation != null) {
            result.add(new AnnotationNameConditionElement(this, annotation));
        }
        if (superName != null) {
            result.add(new SuperTypeNameConditionElement(this, superName));
        }
        if (result.isEmpty()) {
            result.add(new AnyConditionElement(this));
        }
        return result;
    }

    public class AnyConditionElement extends org.openclover.eclipse.core.projects.settings.source.test.AnyConditionElement {
        public AnyConditionElement(TreeElement parent) {
            super(parent);
        }
    }
}
