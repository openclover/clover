package org.openclover.core.reporters.json;

import clover.com.google.gson.Gson;
import clover.com.google.gson.GsonBuilder;
import clover.org.apache.velocity.VelocityContext;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.reporters.CloverReportConfig;
import org.openclover.core.reporters.html.HtmlRenderingSupportImpl;
import org.openclover.core.reporters.html.HtmlReportUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

public class RenderTreeMapAction implements Callable<Object> {

    private final ProjectInfo project;
    private final File outdir;
    private final VelocityContext mContext;
    private final HtmlRenderingSupportImpl renderSupport = new HtmlRenderingSupportImpl();
    private final CloverReportConfig reportConfig;

    public RenderTreeMapAction(VelocityContext context, CloverReportConfig reportConfig, File outdir, ProjectInfo project) {
        this.project = project;
        this.outdir = outdir;
        this.mContext = context;
        this.reportConfig = reportConfig;
    }

    /**
     * Generate JSON to be used by the treemap in <a href="http://thejit.org/docs/files/Treemap-js.html">The Jit</a>.
     *
     * @return the json string that was created
     */
    @Override
    public Object call() throws Exception {

        final String jsonStr = renderTreeMapJson("treemap-json.js", "processTreeMapJson", true);
        final String filename = "treemap.html";
        mContext.put("projectInfo", project);
        mContext.put("currentPageURL", filename);

        mContext.put("headerMetrics", project.getMetrics());
        mContext.put("headerMetricsRaw", project.getRawMetrics());
        mContext.put("appPagePresent", Boolean.TRUE);
        mContext.put("testPagePresent", Boolean.TRUE);
        mContext.put("topLevel", Boolean.TRUE);

        HtmlReportUtil.mergeTemplateToFile(new File(outdir, filename), mContext,
                "treemap.vm");

        return jsonStr;
    }

    public String renderTreeMapJson(String filename, String callback, boolean classLevel) throws Exception {
        final String jsonStr = generateJson(classLevel);
        mContext.put("callback", callback);
        mContext.put("json", jsonStr);
        HtmlReportUtil.mergeTemplateToFile(new File(outdir, filename), mContext,
                "treemap-json.vm");
        return jsonStr;
    }

    String generateJson(boolean classLevel) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        final List<PackageInfo> pkgInfos = project.getAllPackages();

        final List<Node> pkgNodes = new ArrayList<>(pkgInfos.size());

        final Node projectNode = createNode(project.getDataIndex(), "", project, pkgNodes);

        for (final PackageInfo packageInfo : pkgInfos) {
            final List<ClassInfo> classes = packageInfo.getClasses(HasMetricsFilter.ACCEPT_ALL);
            // create a package node.
            final List<Node> classesList = new ArrayList<>(classes.size());
            pkgNodes.add(createNode(packageInfo.getDataIndex(), packageInfo.getName(), packageInfo, classesList));

            for (Iterator<ClassInfo> iterator = classes.iterator(); classLevel && iterator.hasNext(); ) {
                final FullClassInfo classInfo = (FullClassInfo) iterator.next();
                // create a leaf node and add to the package's children list
                final String path = classInfo.getContainingFile() != null ?
                        renderSupport.getSrcFileLink(true, true, classInfo).toString() : null;
                classesList.add(createNode(classInfo.getDataIndex(), classInfo.getName(), classInfo, Collections.emptyList(), path)); // TreeMap requires the children:[] ele
            }
        }

        return gson.toJson(projectNode);
    }

    private Node createNode(int index, String nodeName, HasMetrics hasMetrics, List<Node> children) {
        return createNode(index, nodeName, hasMetrics, children, null);
    }
    
    private Node createNode(int index, String nodeName, HasMetrics hasMetrics, List<Node> children, String path) {
        final BlockMetrics metrics = hasMetrics.getMetrics();

        final String pcStr = renderSupport.getPercentStr(metrics.getPcCoveredElements());
        final String title = String.format("%s %s Elements, %s Coverage",
                                            nodeName, metrics.getNumElements(), pcStr); 
        
        final Data data = new Data(metrics.getNumElements(), metrics.getPcCoveredElements() * 100f, path, title);
        return new Node(hasMetrics.getName() + index , nodeName, data, children);
    }


    /**
     * A class to model the json format required by <a href="http://thejit.org/docs/files/Treemap-js.html">Treemap-js</a>
     *
     *<pre>
     * var json = {
     * "id": "aUniqueIdentifier",
     * "name": "usually a nodes name",
     * "data": {
     *      "$area": 33, //some float value
     *      "$color": 36, //-optional- some float value
     *      "path": "/path/to/file.html",
     *      "title": "ClassName NumElements, Coverage" // mouseover title.
     *      },
     * "children": [ 'other nodes or empty' ]
     * };
     * </pre>
     *
     */
    private static class Node {
        final String id;
        final String name;
        final Data data;
        final Collection<Node> children;

        private Node(String id, String name, Data data, Collection<Node> children) {
            this.id = id;
            this.name = name;
            this.data = data;
            this.children = children;
        }
    }

    private static class Data {
        final float $area;
        final float $color;
        final String path; // path to src-file.html
        final String title; // title for mouseover

        private Data(float area, float color, String path, String title) {
            this.$area = area;
            this.$color = color;
            this.path = path;
            this.title = title;
        }
    }
}
