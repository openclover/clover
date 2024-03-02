package org.openclover.core.registry.entities;

import org.jetbrains.annotations.NotNull;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.EntityVisitor;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.api.registry.IsCacheable;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.api.registry.CoverageDataReceptor;
import org.openclover.core.api.registry.FileInfoVisitor;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.core.registry.metrics.HasMetricsNode;
import org.openclover.core.registry.metrics.PackageMetrics;
import org.openclover.core.registry.metrics.ProjectMetrics;
import org.openclover.core.util.Path;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Maps.newTreeMap;


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
public class FullProjectInfo
        implements ProjectInfo, HasMetricsNode, CoverageDataReceptor, IsCacheable {

    private int dataIndex = 0;
    private int dataLength;

    protected String name;
    protected Map<String, PackageInfo> packages;
    protected long version;

    protected Map<String, ClassInfo> classLookup;
    protected Map<String, FileInfo> fileLookup;
    protected BlockMetrics rawMetrics;
    protected BlockMetrics metrics;
    protected ContextSet contextFilter;

    private List<PackageInfo> orderedPkgs;
    private List<PackageFragment> orderedPkgRoots;
    private Map<String, PackageFragment> roots;
    private boolean fragmented;
    private Comparator<HasMetrics> orderby;
    private CoverageDataProvider data;
    private boolean hasTestResults; // true if the model has at least one test result

    public FullProjectInfo(String name, long version) {
        this.name = name;
        this.version = version;
        this.packages = new LinkedHashMap<>();
    }

    public FullProjectInfo(String name) {
        this(name, System.currentTimeMillis());
    }

    // ProjectInfo


    /**
     * convenience method to find a class in a project using its fully qualified name. Initialized lazily, so
     * the first call may be slow
     * @param fqcn a fully qualified class name
     * @return corresponding BaseClassInfo or null if not found
     */
    @Override
    public ClassInfo findClass(String fqcn) {
        if (classLookup == null) {
            buildClassLookupMap();
        }
        return classLookup.get(fqcn);
    }

    /**
     * convenience method to find a file in a project using its package path.  Initialized lazily, so
     * the first call may be slow
     * @param pkgPath - path of the file to look for
     * @return corresponding BaseFileInfo or null if not found
     */
    @Override
    public FileInfo findFile(String pkgPath) {
        if (fileLookup == null) {
            buildFileLookupMap();
        }
        return fileLookup.get(pkgPath);
    }

    @Override
    @NotNull
    public List<PackageInfo> getAllPackages() {
        return newArrayList(packages.values());
    }

    @Override
    public PackageInfo findPackage(String name) {
        return getNamedPackage(name);
    }

    // CachingInfo

    @Override
    public void invalidateCaches() {
        classLookup = null;
        fileLookup = null;
        rawMetrics = null;
        metrics = null;
        orderedPkgs = null;
        orderedPkgRoots = null;
        roots = null;
    }

    // EntityContainer

    /**
     * Visit yourself
     *
     * @param entityVisitor callback
     */
    @Override
    public void visit(EntityVisitor entityVisitor) {
        entityVisitor.visitProject(this);
    }

    // HasContextFilter

    @Override
    public ContextSet getContextFilter() {
        return contextFilter;
    }

    // HasMetricsNode

    @Override
    public String getChildType() {
        return "package";
    }

    @Override
    public boolean isEmpty() {
        return packages.isEmpty();
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
    public void setComparator(Comparator<HasMetrics> cmp) {
        orderby = cmp;
        roots = null;
        orderedPkgs = null;
        orderedPkgRoots = null;
        for (PackageInfo packageInfo : packages.values()) {
            packageInfo.setComparator(cmp);
        }

    }

    // HasMetrics

    @Override
    public String getName() {
        return name;
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

    @Override
    public void setMetrics(BlockMetrics metrics) {
        this.metrics = metrics;
    }

    // HasVersions

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(final long version) {
        this.version = version;
        visitFiles(file -> ((FullFileInfo)file).addVersion(version));
    }


    // OTHER

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void addPackage(PackageInfo pkg) {
        packages.put(pkg.getName(), pkg);
    }

    public PackageInfo getDefaultPackage() {
        return packages.get(PackageInfo.DEFAULT_PACKAGE_NAME);
    }

    @Override
    public PackageInfo getNamedPackage(String name) {
        if (name == null || name.length() == 0 || PackageInfo.DEFAULT_PACKAGE_NAME.equals(name)) {
            return getDefaultPackage();
        }
        return packages.get(name);
    }

    /**
     * convenience method to get all classes in a project that meet some criteria
     * @param filter filter to apply
     * @return list of classes that match filter
     */
    @Override
    public List<ClassInfo> getClasses(HasMetricsFilter filter) {
        if (classLookup == null) {
            buildClassLookupMap();
        }

        List<ClassInfo> result = newArrayList();
        for (ClassInfo classInfo : classLookup.values()) {
            if (filter.accept(classInfo)) {
                result.add(classInfo);
            }
        }
        return result;
    }

    /**
     * convenience method to get all filees in a project that meet some criteria
     * @param filter filter to apply
     * @return list of files that match filter
     */
    @Override
    public List<FileInfo> getFiles(HasMetricsFilter filter) {
        if (fileLookup == null) {
            buildFileLookupMap();
        }
        List<FileInfo> result = newArrayList();
        for (FileInfo fileInfo : fileLookup.values()) {
            if (filter.accept(fileInfo)) {
                result.add(fileInfo);
            }
        }
        return result;
    }

    @Override
    public List<PackageInfo> getPackages(HasMetricsFilter filter) {
        List<PackageInfo> result = newArrayList();
        for (PackageInfo packageInfo : packages.values()) {
            if (filter.accept(packageInfo)) {
                result.add(packageInfo);
            }
        }
        return result;
    }

    private void buildClassLookupMap() {
        final Map<String, ClassInfo> tmpClassLookup = new LinkedHashMap<>();
        visitFiles(file -> {
            for (ClassInfo info : file.getClasses()) {
                tmpClassLookup.put(info.getQualifiedName(), info);
            }
        });
        classLookup = tmpClassLookup;
    }

    private void buildFileLookupMap() {
        final Map<String, FileInfo> tmpFileLookup = new LinkedHashMap<>();
        visitFiles(file -> tmpFileLookup.put(file.getPackagePath(), file));
        fileLookup = tmpFileLookup;
    }

    @Override
    public void setContextFilter(ContextSet filter) {
        contextFilter = filter;
        metrics = null;
    }

    @Override
    public void visitFiles(FileInfoVisitor visitor) {
        for (PackageInfo pkgInfo : packages.values()) {
            pkgInfo.visitFiles(visitor);
        }
    }

    @Override
    public PackageFragment[] getPackageRoots() {
        ensureRootsBuilt();
        return (PackageFragment[]) roots.values().toArray(new PackageFragment[0]);
    }

    @Override
    public ProjectInfo copy() {
        return copy(HasMetricsFilter.ACCEPT_ALL);
    }

    /**
     * create a deep copy of this project tree, applying the filter to all levels
     * @return  filtered copy of the projectinfo
     */
    @Override
    public ProjectInfo copy(HasMetricsFilter filter) {
        return copy(filter, getContextFilter());
    }

    /**
     * create a deep copy of this project tree, applying the filter and context set to all levels
     * @return  filtered copy of the projectinfo
     */
    @Override
    public ProjectInfo copy(HasMetricsFilter filter, ContextSet contextFilter) {
        ProjectInfo proj = new FullProjectInfo(name);
        proj.setContextFilter(contextFilter);
        proj.setDataProvider(getDataProvider());
        proj.setVersion(getVersion());
        for (PackageInfo pkgInfo : packages.values()) {
            if (filter.accept(pkgInfo)) {
                PackageInfo info = pkgInfo.copy(proj, filter);
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
        TreeMap<String, PackageFragment> tmpRoots = newTreeMap(); // natural ordering
        List<PackageFragment> tmpOrderedPkgRoots = newArrayList();
        for (PackageInfo packageInfo : packages.values()) {
            addPackageToTree(packageInfo, tmpRoots, tmpOrderedPkgRoots);
        }
        
        if (orderby != null) {
            tmpOrderedPkgRoots.sort(orderby);
        }
        orderedPkgRoots = tmpOrderedPkgRoots;
        roots = tmpRoots;
    }

    private void buildOrderedPackageList() {
        List<PackageInfo> tmpOrderedPkgs = newArrayList(packages.values());
        if (orderby != null) {
            tmpOrderedPkgs.sort(orderby);
        }
        orderedPkgs = tmpOrderedPkgs;
    }

    private void addPackageToTree(PackageInfo pkg, final Map<String, PackageFragment> roots, final List<PackageFragment> orderedPkgRoots) {
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

    @Override
    public void resolve(final Path sourcePath) {
        visitFiles(file -> ((FullFileInfo)file).resolve(sourcePath));
    }

    @Override
    public void setDataProvider(CoverageDataProvider data) {
        this.data = data;
        for (PackageInfo packageInfo : packages.values()) {
            packageInfo.setDataProvider(data);
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

    @Override
    public void setDataLength(int length) {
        dataLength = length;
    }

    @Override
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
    public boolean hasTestResults() {
        return hasTestResults;
    }

    @Override
    public void setHasTestResults(boolean hasTestResults) {
        this.hasTestResults = hasTestResults;
    }

    private ProjectMetrics calcMetrics(boolean filter) {
        ProjectMetrics projectMetrics = new ProjectMetrics(this);
        int numPackages = 0;
        for (PackageInfo packageInfo : packages.values()) {
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

    @Override
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

}
