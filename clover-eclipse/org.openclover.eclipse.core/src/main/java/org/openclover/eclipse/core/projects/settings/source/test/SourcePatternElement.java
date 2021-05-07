package org.openclover.eclipse.core.projects.settings.source.test;

public class SourcePatternElement implements TreeElement {
    private TestPackageRootElement parent;
    private String pattern;

    public SourcePatternElement(TestPackageRootElement parent, String pattern) {
        this.parent = parent;
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    public boolean isAll() {
        return pattern.equals("**/*.java") || pattern.equals("**/*");
    }

    public boolean isEmpty() {
        return pattern.equals("");
    }

    @Override
    public TreeElement getParent() {
        return parent;
    }
}
