package org.openclover.core.spec.instr.test;

import java.io.Serializable;

public class TestMethodSpec implements Serializable {
    private String annotation;
    private String name;
    private String returnType;
    private String tag;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
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

    @Override
    public String toString() {
        return "testmethod("
                + (annotation != null ? " annotation=" + annotation : "")
                + (name != null ? " name=" + name : "")
                + (tag != null ? " tag=" + tag : "")
                + (returnType != null ? " returntype=" + returnType : "")
                + ")";
    }
}
