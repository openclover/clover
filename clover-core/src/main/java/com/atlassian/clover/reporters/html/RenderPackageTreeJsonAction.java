package com.atlassian.clover.reporters.html;

import clover.org.apache.velocity.VelocityContext;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.registry.metrics.HasMetricsSupport;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.util.trie.PackagePrefixTree;
import com.atlassian.clover.util.trie.PrefixTree;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Generate tree of all packages available in the project with compressed empty packages in a JSON format.
 */
public class RenderPackageTreeJsonAction implements Callable {

    private final FullProjectInfo fullProjectInfo; // shared - read only
    private final FullProjectInfo appProjectInfo;  // shared - read only
    private final VelocityContext context; // not shared, read/write
    private final Current reportConfig;
    private final File basePath;


    public RenderPackageTreeJsonAction(VelocityContext ctx, File basePath,
                                       FullProjectInfo fullProjectInfo, FullProjectInfo appProjectInfo,
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

    protected File insertPackageTreeProperties() throws Exception {
        final PrefixTree packageTree = createTreeFromList(collectAllPackagesByName());

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
        final List<? extends PackageInfo> packages = fullProjectInfo.getAllPackages();
        Collections.sort(packages, HasMetricsSupport.LEX_COMP);

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