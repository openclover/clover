package com.atlassian.clover.registry.entities;

import com.atlassian.clover.api.registry.BlockMetrics;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.api.registry.ProjectInfo;
import com.atlassian.clover.registry.CoverageDataProvider;
import com.atlassian.clover.registry.CoverageDataReceptor;
import com.atlassian.clover.registry.FileInfoVisitor;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.registry.metrics.HasMetricsNode;
import com.atlassian.clover.registry.metrics.PackageMetrics;
import com.atlassian.clover.registry.metrics.ProjectMetrics;
import com.atlassian.clover.util.Path;
import com.atlassian.clover.context.ContextSet;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import static clover.com.google.common.collect.Lists.newArrayList;
import static clover.com.google.common.collect.Maps.newTreeMap;


/**
 *  <p>A model of a java project. The model is built at instrumentation time and can then be queried to generate
 *  reports.</p>
 *
 *  <h3>Instrumentation time</h3>
 *  <p>The project model is updated via the various <code>add*()</code> methods. The project must be versioned using
 *  <code>setVersion()</code> at the start of each instrumentation session. @see Clover2Registry.
 *  </p>
 *  <h3>Reporting time</h3>
 *  <p>The main entry points to project hierachy are the <code>getPackages()</code>, <code>getPackages(HasMetricsFilter)</code> methods,
 *  which return arrays of <code>FullPackageInfo</code>s for the project.
 *  </p>
 *  <p>
 *  Several convenience methods are provided to enable fast lookup of entities in the project, using lazily built data structures:
 *  <ul>
 *
 *  <li><code>getPackageRoots()</code> returns a tree of <code>PackageFragment</code>s representing the (possibly) non-concrete
 *  packages in the project.</li>
 *  <li><code>findClass()</code> finds a <code>ClassInfo</code> in the project given its fully qualified name.</li>
 *  <li><code>hasTestHits(HasMetricsNode), getTestHits(HasMetricsNode),</code> and <code>getTestMethod(int)</code> gets information on
 *  which tests hit a given entity in the project. </li>
 * </p>
 *  <p>
 *  Metrics for the project are available via <code>getMetrics()</code>. Note that the returned <code>BlockMetrics</code>
 *  should be cast to a <code>ProjectMetrics</code> to access project-level metrics. Before querying coverage related metrics on
 *  the project or its children, you must set a <code>CoverageDataProvider</code> (otherwise all coverage-related
 *  metrics will return 0).
 *  </p>
 *  <p>A call to <code>setDataProvider()</code> forces metrics for the project and all children to be lazily
 *  recalculated.</p>
 *  <p><strong>Note: The <code>setComparator()</code> method only affects the order of project elements returned via the
 *  <code>HasMetricsNode</code> interface</strong></p>
 *  <p><strong>Note: The serialized version of this class is used in the stored version of the <code>Clover2Registry</code>.
 *  Care must be taken when adding non-transient fields to this class, because they could potentially break
 *  compatibility with existing registry files.</strong></p>
 *
 */
public class FullProjectInfo extends BaseProjectInfo implements HasMetricsNode, CoverageDataReceptor, ProjectInfo {
    private int dataIndex = 0;
    private int dataLength;

    private List orderedPkgs;
    private List orderedPkgRoots;
    private Map roots;
    private boolean fragmented;
    private Comparator orderby;
    private CoverageDataProvider data;
    private boolean hasTestResults; // true if the model has at least one test result

    public FullProjectInfo(String name, long version) {
        super(name, version);
    }

    public FullProjectInfo(String name) {
        super(name);
    }

    public PackageFragment[] getPackageRoots() {
        ensureRootsBuilt();
        return (PackageFragment[]) roots.values().toArray(new PackageFragment[0]);
    }

    public FullProjectInfo copy() {
        return copy(HasMetricsFilter.ACCEPT_ALL);
    }

    /**
     * create a deep copy of this project tree, applying the filter to all levels
     * @return  filtered copy of the projectinfo
     */
    public FullProjectInfo copy(HasMetricsFilter filter) {
        return copy(filter, getContextFilter());
    }

    /**
     * create a deep copy of this project tree, applying the filter and context set to all levels
     * @return  filtered copy of the projectinfo
     */
    public FullProjectInfo copy(HasMetricsFilter filter, ContextSet contextFilter) {
        FullProjectInfo proj = new FullProjectInfo(name);
        proj.setContextFilter(contextFilter);
        proj.setDataProvider(getDataProvider());
        proj.setVersion(getVersion());
        for (BasePackageInfo basePackageInfo : packages.values()) {
            FullPackageInfo pkgInfo = (FullPackageInfo) basePackageInfo;
            if (filter.accept(pkgInfo)) {
                FullPackageInfo info = pkgInfo.copy(proj, filter);
                if (!info.isEmpty()) {
                    proj.addPackage(info);
                }
            }
        }
        proj.setDataLength(getDataLength());
        proj.setHasTestResults(hasTestResults());
        return proj;
    }

    private void buildPackageTrees() {
        TreeMap tmpRoots = newTreeMap(); // natural ordering
        List tmpOrderedPkgRoots = newArrayList();
        for (BasePackageInfo basePackageInfo : packages.values()) {
            FullPackageInfo packageInfo = (FullPackageInfo) basePackageInfo;
            addPackageToTree(packageInfo, tmpRoots, tmpOrderedPkgRoots);
        }
        
        if (orderby != null) {
            Collections.sort(tmpOrderedPkgRoots, orderby);
        }
        orderedPkgRoots = tmpOrderedPkgRoots;
        roots = tmpRoots;
    }

    private void buildOrderedPackageList() {
        List tmpOrderedPkgs = newArrayList(packages.values());
        if (orderby != null) {
            Collections.sort(tmpOrderedPkgs, orderby);
        }
        orderedPkgs = tmpOrderedPkgs;
    }

    private void addPackageToTree(FullPackageInfo pkg, final Map roots, final List orderedPkgRoots) {
        StringTokenizer pkgfragments = new StringTokenizer(pkg.getName(), ".");
        String qname = "";
        String sep = "";
        PackageFragment currentFrag = null;

        while (pkgfragments.hasMoreTokens()) {
            String frag =  pkgfragments.nextToken();
            qname += sep + frag;
            sep = ".";
            if (currentFrag == null) {
                // check root level
                PackageFragment root = (PackageFragment)roots.get(frag);
                if (root == null) {
                    root = new PackageFragment(null, this, frag, qname);
                    root.setComparator(orderby);
                    roots.put(frag, root);
                    orderedPkgRoots.add(root);
                }
                currentFrag = root;
            }
            else {
                PackageFragment node = currentFrag.getChild(frag);
                if (node == null) {
                    node = new PackageFragment(currentFrag, this, qname, frag);
                    node.setComparator(orderby);
                    currentFrag.addChild(node);
                }
                currentFrag = node;
            }
        }
        if (currentFrag != null) {
            currentFrag.setConcretePackage(pkg);
        }
    }

    public void resolve(final Path sourcePath) {
        visitFiles(new FileInfoVisitor() {
            @Override
            public void visitFileInfo(BaseFileInfo file) {
                 ((FullFileInfo)file).resolve(sourcePath);
            }
        });
    }

    @Override
    public void setDataProvider(CoverageDataProvider data) {
        this.data = data;
        for (BasePackageInfo basePackageInfo : packages.values()) {
            FullPackageInfo pkgInfo = (FullPackageInfo) basePackageInfo;
            pkgInfo.setDataProvider(data);
        }
        rawMetrics = null;
        metrics = null;
    }

    @Override
    public CoverageDataProvider getDataProvider() {
        return data;
    }

    @Override
    public int getDataIndex() {
        return dataIndex;
    }

    @Override
    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int length) {
        dataLength = length;
    }

    public void buildCaches() {
        buildOrderedPackageList();
        buildPackageTrees();
    }

    public boolean isFragmented() {
        return fragmented;
    }

    public void setFragmented(boolean fragmented) {
        this.fragmented = fragmented;
    }

    @Override
    public String getChildType() {
        return "package";
    }

    @Override
    public int getNumChildren() {

        if (fragmented) {
            ensureRootsBuilt();
            return roots.size();
        }

        if (orderedPkgs == null) {
            buildOrderedPackageList();
        }

        return orderedPkgs.size();
    }

    @Override
    public HasMetricsNode getChild(int i) {

        if (fragmented) {
            ensureRootsBuilt();
            return (HasMetricsNode)orderedPkgRoots.get(i);
        }

        if (orderedPkgs == null) {
            buildOrderedPackageList();
        }
        // todo - bounds checking?
        return (HasMetricsNode)orderedPkgs.get(i);
    }

    @Override
    public int getIndexOfChild(HasMetricsNode child) {
        if (fragmented) {
            ensureRootsBuilt();
            return orderedPkgs.indexOf(child.getName());
        }
        if (orderedPkgs == null) {
            buildOrderedPackageList();
        }
        return orderedPkgs.indexOf(child.getName());
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public void setComparator(Comparator cmp) {
        orderby = cmp;
        roots = null;
        orderedPkgs = null;
        orderedPkgRoots = null;
        for (BasePackageInfo basePackageInfo : packages.values()) {
            FullPackageInfo packageInfo = (FullPackageInfo) basePackageInfo;
            packageInfo.setComparator(cmp);
        }

    }

    @Override
    public void setVersion(final long version) {
        super.setVersion(version);
        visitFiles(new FileInfoVisitor() {
            @Override
            public void visitFileInfo(BaseFileInfo file) {
                 ((FullFileInfo)file).addVersion(version);
            }
        });
    }

    @Override
    public BlockMetrics getMetrics() {
        if (metrics == null) {
            metrics = calcMetrics(true);
        }
        return metrics;
    }
    
    @Override
    public BlockMetrics getRawMetrics() {
        if (rawMetrics == null) {
            rawMetrics = calcMetrics(false);
        }
        return rawMetrics;

    }

    public boolean hasTestResults() {
        return hasTestResults;
    }

    public void setHasTestResults(boolean hasTestResults) {
        this.hasTestResults = hasTestResults;
    }

    private ProjectMetrics calcMetrics(boolean filter) {
        ProjectMetrics projectMetrics = new ProjectMetrics(this);
        int numPackages = 0;
        for (BasePackageInfo basePackageInfo : packages.values()) {
            FullPackageInfo packageInfo = (FullPackageInfo) basePackageInfo;
            if (!filter) {
                projectMetrics.add((PackageMetrics) packageInfo.getRawMetrics());
            } else {
                projectMetrics.add((PackageMetrics) packageInfo.getMetrics());
            }
            numPackages++;
        }
        projectMetrics.setNumPackages(numPackages);
        return projectMetrics;
    }

    private void ensureRootsBuilt() {
        if (roots == null)  {
            buildPackageTrees();
        }
    }

    public PackageFragment findPackageFragment(String packageName) {
        final String[] names = packageName.split("\\.");
        PackageFragment[] fragments = getPackageRoots();
        PackageFragment result = null;
        for (String name : names) {
            PackageFragment currentFragment = null;
            for (PackageFragment fragment : fragments) {
                if (fragment.getName().equals(name)) {
                    currentFragment = fragment;
                    break;
                }
            }
            result = currentFragment;
            if (currentFragment == null) {
                break;
            } else {
                fragments = currentFragment.getChildren();
            }
        }
        return result;
    }

    @Override
    public void invalidateCaches() {
        super.invalidateCaches();
        orderedPkgs = null;
        orderedPkgRoots = null;
        roots = null ;
    }

    @Override
    public PackageInfo findPackage(String name) {
        return getNamedPackage(name);
    }

}
