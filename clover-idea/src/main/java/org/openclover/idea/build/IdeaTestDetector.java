package org.openclover.idea.build;

import com.atlassian.clover.instr.tests.TestDetector;
import com.atlassian.clover.instr.tests.DefaultTestDetector;
import org.openclover.idea.IdeaTestFilter;
import com.intellij.openapi.project.Project;

public class IdeaTestDetector extends IdeaTestFilter implements TestDetector {
    private final TestDetector defaultTestDetector = new DefaultTestDetector();

    private IdeaTestDetector(boolean inverted, Project project) {
        super(inverted, project);
    }

    public IdeaTestDetector(Project project) {
        this(false, project);
    }

    @Override
    public IdeaTestDetector invert() {
        return new IdeaTestDetector(!isInverted(), project);
    }

    @Override
    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext) {
        return defaultTestDetector.isTypeMatch(sourceContext, typeContext)
                || isInTestFolder(sourceContext.getSourceFile());
    }

    @Override
    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext) {
        return defaultTestDetector.isMethodMatch(sourceContext, methodContext);
    }

}
