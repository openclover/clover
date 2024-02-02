package org.openclover.idea.build.jps;

import com.atlassian.clover.util.trie.FilePathPrefixTree;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.java.LanguageLevel;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.io.File;

/**
 * Utility methods for getting various data from JpsProject in a PrefixTree data structure.
 */
public class JpsProjectPrefixTreeUtil {

    /**
     * Analyze current project and store (source root &lt;-&gt; language level) mapping
     *
     * @param project current project
     * @return FilePathPrefixTree&lt;LanguageLevel&gt;
     */
    public static FilePathPrefixTree<LanguageLevel> collectLanguageLevels(final JpsProject project) {
        // put the latest support language level JDK_1_7 into the root
        final FilePathPrefixTree<LanguageLevel> languageLevelTrie =
                new FilePathPrefixTree<>(LanguageLevel.JDK_1_7);
        final JpsJavaExtensionService javaExt = JpsJavaExtensionService.getInstance();

        for (final JpsModule module : project.getModules()) {
            final LanguageLevel moduleLevel = javaExt.getLanguageLevel(module);
            for (final JpsModuleSourceRoot moduleSourceRoot : module.getSourceRoots()) {
                languageLevelTrie.add(moduleSourceRoot.getFile(), moduleLevel);
            }
        }

        return languageLevelTrie;
    }

    /**
     * Analyze current project and store (source root &lt;-&gt; enclosing module) mapping
     *
     * @param project current project
     * @return FilePathPrefixTree&lt;JpsModule&gt;
     */
    public static FilePathPrefixTree<JpsModule> collectModules(final JpsProject project) {
        final FilePathPrefixTree<JpsModule> moduleTrie = new FilePathPrefixTree<>();

        for (final JpsModule module : project.getModules()) {
            for (final JpsModuleSourceRoot moduleSourceRoot : module.getSourceRoots()) {
                moduleTrie.add(moduleSourceRoot.getFile(), module);
            }
        }

        return moduleTrie;
    }

    /**
     * Analyze current project and store (source root &lt;-&gt; root type) mapping. Root type can be usually one of:
     * JavaSourceRootType.TEST_SOURCE, JavaSourceRootType.SOURCE.
     *
     * @param project current project
     * @return FilePathPrefixTree&lt;JpsModuleSourceRootType&gt;
     */
    public static FilePathPrefixTree<JpsModuleSourceRootType> collectRootTypes(final JpsProject project) {
        final FilePathPrefixTree<JpsModuleSourceRootType> moduleTrie = new FilePathPrefixTree<>();

        // collect app/test source roots
        for (final JpsModule module : project.getModules()) {
            for (final JpsModuleSourceRoot moduleSourceRoot : module.getSourceRoots()) {
                moduleTrie.add(moduleSourceRoot.getFile(), moduleSourceRoot.getRootType());
            }
        }

        return moduleTrie;
    }

    /**
     * Analyze current project and store (source root &lt;-&gt; is excluded) mapping.
     *
     * @param project current project
     * @return FilePathPrefixTree&lt;Boolean&gt;
     */
    public static FilePathPrefixTree<Boolean> collectExcludedRoots(final JpsProject project) {
        final FilePathPrefixTree<Boolean> moduleTrie = new FilePathPrefixTree<>(Boolean.FALSE);

        // collect excluded source roots
        for (final JpsModule module : project.getModules()) {
            for (final File excludedRoot : JpsModelUtil.getExcludedRoots(module)) {
                moduleTrie.add(excludedRoot, Boolean.TRUE);
            }
        }

        return moduleTrie;
    }

}
