package com.atlassian.clover.eclipse.core.projects.settings.source;

import org.eclipse.jdt.core.IPackageFragmentRoot;

public class SourceRootWithPattern {
    private SourceFolderPattern pattern;
    private IPackageFragmentRoot pfRoot;

    public SourceFolderPattern getPattern() {
        return pattern;
    }

    public SourceRootWithPattern(IPackageFragmentRoot pfRoot, SourceFolderPattern pattern) {
        this.pattern = pattern;
        this.pfRoot = pfRoot;
    }

    public void setPattern(SourceFolderPattern pattern) {
        this.pattern = pattern;
    }

    public IPackageFragmentRoot getPfRoot() {
        return pfRoot;
    }

    public void setPfRoot(IPackageFragmentRoot pfRoot) {
        this.pfRoot = pfRoot;
    }
}
