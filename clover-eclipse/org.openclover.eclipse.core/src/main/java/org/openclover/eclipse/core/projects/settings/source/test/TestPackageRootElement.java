package org.openclover.eclipse.core.projects.settings.source.test;

import com.atlassian.clover.instr.tests.TestDetector;
import com.atlassian.clover.instr.tests.TestSourceMatcher;
import com.atlassian.clover.instr.tests.DefaultTestDetector;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.spec.instr.test.BooleanSpec;
import org.openclover.eclipse.core.CloverPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.util.Collections;

public class TestPackageRootElement implements TreeElement {
    private TestSourcesElement parent;
    private IPath projectRelativePath;
    private SourcePatternElement includes = new SourceIncludePatternElement(this, "**/*.java");
    private SourcePatternElement excludes = new SourceExcludePatternElement(this);
    private ExpressionElement expression = new OrExpressionElement(this);

    public TestPackageRootElement(TestSourcesElement parent, String projectRelativePath) {
        this.parent = parent;
        this.projectRelativePath = new Path(projectRelativePath);
    }

    public void setProjectRelativePath(String projectRelativePath) {
        this.projectRelativePath = new Path(projectRelativePath);
    }

    public void setIncludes(SourcePatternElement includes) {
        this.includes = includes;
    }

    public void setExcludes(SourcePatternElement excludes) {
        this.excludes = excludes;
    }

    public SourcePatternElement getIncludes() {
        return includes;
    }

    public SourcePatternElement getExcludes() {
        return excludes;
    }

    public IPath getProjectRelativePath() {
        return projectRelativePath;
    }

    public ExpressionElement getExpression() {
        return expression;
    }

    public void setExpression(ExpressionElement expression) {
        this.expression = expression;
    }

    public TestSourceMatcher getTestSourceMatcher() {
        return new TestSourceMatcher() {
            @Override
            public boolean matchesFile(File f) {
                //TODO
                return false;
            }

            @Override
            public TestDetector getDetector() {
                try {
                    return BooleanSpec.buildTestDetectorFor(Collections.singletonList(expression.getSpec()));
                } catch (CloverException e) {
                    CloverPlugin.logError("Unable to build test detector", e);
                    return new DefaultTestDetector();
                }
            }
        };
    }

    @Override
    public TreeElement getParent() {
        return parent;
    }
}
