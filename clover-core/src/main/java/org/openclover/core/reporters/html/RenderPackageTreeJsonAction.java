package org.openclover.core.reporters.html;

import clover.org.apache.velocity.VelocityContext;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.registry.metrics.HasMetricsSupport;
import org.openclover.core.reporters.Current;
import org.openclover.core.util.trie.PackagePrefixTree;
import org.openclover.core.util.trie.PrefixTree;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Generate tree of all packages available in the project with compressed empty packages in a JSON format.
 */
public class RenderPackageTreeJsonAction implements Callable<Object> {

    private final ProjectInfo fullProjectInfo; // shared - read only
    private final ProjectInfo appProjectInfo;  // shared - read only
    private final VelocityContext context; // not shared, read/write
    private final Current reportConfig;
    private final File basePath;


    public RenderPackageTreeJsonAction(VelocityContext ctx, File basePath,
                                       ProjectInfo fullProjectInfo, ProjectInfo appProjectInfo,
                                       Current reportConfig) {
        this.basePath = basePath;
        this.fullProjectInfo = fullProjectInfo;
        this.appProjectInfo = appProjectInfo;
        this.context = ctx;
        this.reportConfig = reportConfig;
    }

    @Override
    public Object call() throws Exception {
        final File outfile = insertPackageTreeProperties();
        HtmlReportUtil.mergeTemplateToFile(outfile, context, "package-nodes-tree.vm");
        return null;
    }

    protected File insertPackageTreeProperties() {
        final PrefixTree<String, PackageInfoExt> packageTree = createTreeFromList(collectAllPackagesByName());

        context.put("projectInfo", fullProjectInfo);
        context.put("packageTree", packageTree.getRootNode());

        final String filename = "package-nodes-tree.js";
        return new File(basePath, filename);
    }

    /**
     * list of packages sorted alphabetically (for vertical navigation / bottom-left frame) with an extra attribute
     * wheteher package exists only in test model (for proper page linking)
     */
    protected List<PackageInfoExt> collectAllPackagesByName() {
        final List<PackageInfo> packages = fullProjectInfo.getAllPackages();
        packages.sort(HasMetricsSupport.LEX_COMP);

        final List<PackageInfoExt> packagesExt = new ArrayList<>(packages.size());
        for (PackageInfo packageInfo : packages) {
            packagesExt.add(new PackageInfoExt(
                    packageInfo,
                    appProjectInfo.findPackage(packageInfo.getName()) == null));
        }
        return packagesExt;
    }

    private PrefixTree<String, PackageInfoExt> createTreeFromList(List<PackageInfoExt> allPackages) {
        // first, build a prefix tree from a list of packages; tree node contains package name fragment as a key
        PackagePrefixTree trie = new PackagePrefixTree();
        for (PackageInfoExt pkgExt : allPackages) {
            trie.add(pkgExt.getPackageInfo().getName(), pkgExt);
        }
        // next, compress empty nodes
        trie.compressTree();
        return trie;
    }

}