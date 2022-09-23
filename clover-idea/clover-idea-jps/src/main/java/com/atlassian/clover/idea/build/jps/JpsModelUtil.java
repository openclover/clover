package com.atlassian.clover.idea.build.jps;

import com.atlassian.clover.idea.config.CloverPluginConfig;
import com.atlassian.clover.util.trie.FilePathPrefixTree;
import com.atlassian.clover.util.trie.PrefixTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.JpsSimpleElement;
import org.jetbrains.jps.model.JpsUrlList;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Class contains helper methods for manipulating JpsModel
 */
public class JpsModelUtil {

    /**
     * Get Clover project-level settings associated with this project (if any) and check if the toggle "build with
     * Clover" is enabled.
     *
     * @param jpsProject project to be checked
     * @return boolean - true if clover is enabled for this project, false otherwise
     */
    public static boolean isBuildWithCloverEnabled(@NotNull final JpsProject jpsProject) {
        // return value from project settings
        final CloverPluginConfig data = getCloverPluginConfig(jpsProject);
        return data != null && data.isBuildWithClover();
    }

    /**
     * Get Clover project-level settings associated with this project (if any) and check if the toggle "Enable Clover"
     * is selected.
     *
     * @param jpsProject project to be checked
     * @return boolean - true if clover is enabled for this project, false otherwise
     */
    public static boolean isCloverEnabled(@NotNull final JpsProject jpsProject) {
        // return value from project settings
        final CloverPluginConfig data = getCloverPluginConfig(jpsProject);
        return data != null && data.isEnabled();
    }

    /**
     * Return whether sources shall be dumped.
     *
     * @param jpsProject project to be checked
     * @return boolean - true if sources shall be dumped, false otherwise
     */
    public static boolean isDumpInstrumentedSources(@NotNull final JpsProject jpsProject) {
        // return value from project settings
        final CloverPluginConfig data = getCloverPluginConfig(jpsProject);
        return data != null && data.isDumpInstrumentedSources();
    }

    /**
     * Fetch Clover configuration from the project metadata.
     */
    @Nullable
    public static CloverPluginConfig getCloverPluginConfig(@NotNull final JpsProject jpsProject) {
        final JpsSimpleElement<CloverPluginConfig> child = jpsProject.getContainer().getChild(
                CloverJpsProjectConfigurationSerializer.CloverProjectConfigurationRole.INSTANCE);
        return child != null ? child.getData() : null;
    }

    /**
     * Search source root under which given <code>sourceFile</code> resides and returns it. It takes into account
     * excluded source root (returns <code>null</code> if file is excluded).
     *
     * @param module     module containing this file
     * @param sourceFile file location
     * @return JpsModuleSourceRoot source root or <code>null</code> if not found or excluded
     */
    @Nullable
    public static JpsModuleSourceRoot findSourceRootForFile(final @NotNull JpsModule module,
                                                            final @NotNull File sourceFile) {
        // take into account excluded source roots, e.g.:
        //   ModuleA
        //     + src           -> isAncestorOf(src,sourceFile)=true, but
        //       + excluded
        //         + Foo.java  -> sourceFile is excluded
        // note that exclusion overrides also all source folders defined below, e.g.:
        //   ModuleA
        //   + excluded
        //     + src
        //       + Foo.java    -> sourceFile is still excluded
        final FilePathPrefixTree<Boolean> excludedRootsTree = new FilePathPrefixTree<>(Boolean.FALSE); // not excluded by default
        for (final File excludedRoot : getExcludedRoots(module)) {
            excludedRootsTree.add(excludedRoot, Boolean.TRUE);
        }
        if (excludedRootsTree.findNearest(sourceFile).getValue() == Boolean.TRUE) {
            // file is excluded in this module
            return null;
        }

        // ok, file is not excluded, now find all source roots and now find the "real" one, i.e. handle a case like:
        // ModuleA
        // + src          -> source root #1
        //   + test       -> source root #2
        //     + Foo.java -> belongs to the 'test' root, not the 'src'
        final FilePathPrefixTree<JpsModuleSourceRoot> sourceRootsTree = new FilePathPrefixTree<>();
        for (final JpsModuleSourceRoot sourceRoot : module.getSourceRoots()) {
            sourceRootsTree.add(sourceRoot.getFile(), sourceRoot);
        }

        // can return null if root for sourceFile is not found
        final PrefixTree.Node<String, JpsModuleSourceRoot> closestRoot = sourceRootsTree.findNearestWithValue(sourceFile);
        return closestRoot != null ? closestRoot.getValue() : null;
    }

    /**
     * Find JspModule under which source sourceFile is located. Takes into account nesting of modules as well as
     * excluded folders.
     *
     * @param project    current project
     * @param sourceFile file to be checked
     * @return JpsModule or <code>null</code> if module not found
     */
    @Nullable
    public static JpsModule findModuleForFile(final JpsProject project, final File sourceFile) {
        // prefix tree(source root -> module) for which the sourceFile matches
        final FilePathPrefixTree<JpsModule> ancestorModules = new FilePathPrefixTree<>();

        // search through all modules
        for (final JpsModule jpsModule : project.getModules()) {
            final JpsModuleSourceRoot sourceRoot = findSourceRootForFile(jpsModule, sourceFile);
            if (sourceRoot != null) {
                ancestorModules.add(sourceRoot.getFile(), jpsModule); // match found!
            }
        }

        // handle a case when we've got several nested modules, e.g.:
        //   ModuleA
        //   + src            ->isAncestorOf(src,sourceFile)=true
        //     + ModuleB
        //       + src        ->isAncestorOf(src,sourceFile)=true
        //         + Foo.java -> belongs to ModuleB, not ModuleA
        // solution: find the deepest module (i.e. it's source root) in a hierarchy
        final PrefixTree.Node<String, JpsModule> closestModule = ancestorModules.findNearestWithValue(sourceFile);
        return closestModule != null ? closestModule.getValue() : null;
    }

    /**
     * Fetch list of excluded source roots for JpsModule.
     *
     * @param jpsModule module
     * @return Set&lt;File&gt;
     */
    public static Set<File> getExcludedRoots(final JpsModule jpsModule) {
        final JpsUrlList excludedRootsAsUrls = jpsModule.getExcludeRootsList();
        final Set<File> excludedRootsAsFiles = new HashSet<>(excludedRootsAsUrls.getUrls().size());
        for (final String url : excludedRootsAsUrls.getUrls()) {
            excludedRootsAsFiles.add(JpsPathUtil.urlToFile(url));
        }
        return excludedRootsAsFiles;
    }

}
