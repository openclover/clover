package com.atlassian.clover.spec.instr.test;

import java.io.Serializable;
import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

public class TestClassSpec implements Serializable {

    private String pkg;
    private String annotation;
    private String name;
    private String superName;
    private List<TestMethodSpec> testMethodSpecs = newArrayList();
    private String tag;


    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getPackage() {
        return pkg;
    }

    public void setPackage(String pkg) {
        this.pkg = pkg;
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

    public String getSuper() {
        return superName;
    }

    public void setSuper(String superName) {
        this.superName = superName;
    }

    public List<TestMethodSpec> getTestMethods() {
        return testMethodSpecs;
    }

    public void addConfiguredTestMethod(TestMethodSpec testMethodSpec) {
        this.testMethodSpecs.add(testMethodSpec);
    }

    @Override
    public String toString() {
        StringBuilder content = new StringBuilder("testclass("
                + (pkg != null ? " package=" + pkg : "")
                + (annotation != null ? " annotation=" + annotation : "")
                + (name != null ? " name=" + name : "")
                + (superName != null ? " super=" + superName : "")
                + (tag != null ? " tag=" + tag : ""));
        if (testMethodSpecs != null) {
            for (final TestMethodSpec testMethod : testMethodSpecs) {
                content.append("\n\t").append(testMethod.toString());
            }
        }
        content.append(")");
        return content.toString();
    }
}
