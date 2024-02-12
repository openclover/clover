package org.openclover.idea.build.jps;

import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.openclover.core.instr.tests.DefaultTestDetector;
import org.openclover.core.instr.tests.TestDetector;
import org.openclover.core.util.trie.FilePathPrefixTree;

import java.io.File;

/**
 * Test detector which checks if given source file is a test file or not. In addition to the standard test detection
 * algorithm, it returns <code>true</code> for every source file which is located under a folder marked as a 'test
 * source root' in IDEA's project settings.
 *
 * See similar {@link org.openclover.idea.build.IdeaTestDetector} for internal build.
 *
 * @see org.openclover.idea.build.IdeaTestDetector
 */
public class JpsProjectTestDetector implements TestDetector {

    /**
     * Internal, default test detector
     */
    private final TestDetector defaultTestDetector = new DefaultTestDetector();

    /**
     * Set of source roots for quick checking whether file might be under a test folder.
     */
    private final FilePathPrefixTree<JpsModuleSourceRootType> allSourceRoots;

    private final JpsProject jpsProject;

    /**
     * Create test detector for current project
     *
     * @param jpsProject current project
     */
    public JpsProjectTestDetector(final JpsProject jpsProject,
                                  final FilePathPrefixTree<JpsModuleSourceRootType> allSourceRootsCache) {
        this.jpsProject = jpsProject;
        this.allSourceRoots = allSourceRootsCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTypeMatch(SourceContext sourceContext, TypeContext typeContext) {
        return defaultTestDetector.isTypeMatch(sourceContext, typeContext)
                || isInTestFolder(sourceContext.getSourceFile());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMethodMatch(SourceContext sourceContext, MethodContext methodContext) {
        return defaultTestDetector.isMethodMatch(sourceContext, methodContext);
    }

    /**
     * Returns true if provided <code>sourceFile</code> is located under the test folder in any of the project's
     * modules.
     *
     * @param sourceFile location to be checked
     * @return boolean - true if in test folder, false otherwise
     */
    protected boolean isInTestFolder(final File sourceFile) {
        // quick check over test roots - good for finding negatives and when folders are not overlapping
        JpsModuleSourceRootType rootType = allSourceRoots.findNearest(sourceFile).getValue();
        if (rootType != null && rootType.equals(JavaSourceRootType.TEST_SOURCE)) {
            // now perform exact matching
            final JpsModule module = JpsModelUtil.findModuleForFile(jpsProject, sourceFile);
            if (module != null) {
                final JpsModuleSourceRoot sourceRoot = JpsModelUtil.findSourceRootForFile(module, sourceFile);
                if (sourceRoot != null) {
                    return sourceRoot.getRootType().equals(JavaSourceRootType.TEST_SOURCE);
                }
            }
        }

        return false;
    }
}
