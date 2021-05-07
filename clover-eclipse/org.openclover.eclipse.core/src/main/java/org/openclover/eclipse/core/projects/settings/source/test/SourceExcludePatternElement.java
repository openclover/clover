package org.openclover.eclipse.core.projects.settings.source.test;

public class SourceExcludePatternElement extends SourcePatternElement {
    public SourceExcludePatternElement(TestPackageRootElement parent, String pattern) {
        super(parent, pattern);
    }

    public SourceExcludePatternElement(TestPackageRootElement parent) {
        super(parent, "");
    }
}
