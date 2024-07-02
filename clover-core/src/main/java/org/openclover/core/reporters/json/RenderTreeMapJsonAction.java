package org.openclover.core.reporters.json;

import org.apache.commons.lang3.StringEscapeUtils;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.reporters.html.HtmlRenderingSupportImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class RenderTreeMapJsonAction {
    public static String generateJson(ProjectInfo project, HtmlRenderingSupportImpl renderSupport, boolean classLevel) {

        final List<PackageInfo> pkgInfos = project.getAllPackages();

        final List<Node> pkgNodes = new ArrayList<>(pkgInfos.size());

        final Node projectNode = createNode(renderSupport, project.getDataIndex(), "", project, pkgNodes);

        for (final PackageInfo packageInfo : pkgInfos) {
            final List<ClassInfo> classes = packageInfo.getClasses(HasMetricsFilter.ACCEPT_ALL);
            // create a package node.
            final List<Node> classesList = new ArrayList<>(classes.size());
            pkgNodes.add(createNode(renderSupport, packageInfo.getDataIndex(), packageInfo.getName(), packageInfo, classesList));

            for (Iterator<ClassInfo> iterator = classes.iterator(); classLevel && iterator.hasNext(); ) {
                final FullClassInfo classInfo = (FullClassInfo) iterator.next();
                // create a leaf node and add to the package's children list
                final String path = classInfo.getContainingFile() != null ?
                        renderSupport.getSrcFileLink(true, true, classInfo).toString() : null;
                classesList.add(
                        createNode(
                                renderSupport, classInfo.getDataIndex(), classInfo.getName(), classInfo, Collections.emptyList(), path)); // TreeMap requires the children:[] ele
            }
        }

        return toJsonObject(projectNode);
    }

    private static String toJsonObject(Node projectNode) {
        return "{" +
                toJsonAttr("id", projectNode.id, true) + "," +
                toJsonAttr("name", projectNode.name, true) + "," +
                toJsonAttr("data", toJsonObject(projectNode.data), false) + "," +
                toJsonAttr("children", toJsonArray(projectNode.children), false)
                + "}";
    }

    private static String toJsonObject(Data projectNodeData) {
        return "{" +
                toJsonAttr("$area", projectNodeData.$area) + "," +
                toJsonAttr("$color", projectNodeData.$color) + "," +
                toJsonAttr("path", projectNodeData.path, true) + "," +
                toJsonAttr("title", projectNodeData.title, true) +
                "}";
    }

    private static String toJsonAttr(String name, float value) {
        return "\"" + name + "\":" + value;
    }

    private static String toJsonAttr(String name, String value, boolean valueInQuotes) {
        if (valueInQuotes) {
            return "\"" + name + "\":\"" + StringEscapeUtils.escapeJson(value) + "\"";
        } else {
            return "\"" + name + "\":" + value;
        }
    }

    private static String toJsonArray(Collection<Node> projectNodes) {
        return "[" +
                projectNodes
                        .stream()
                        .map(RenderTreeMapJsonAction::toJsonObject)
                        .collect(Collectors.joining(",")) +
                "]";
    }

    private static Node createNode(HtmlRenderingSupportImpl renderSupport, int index, String nodeName,
                                   HasMetrics hasMetrics, List<Node> children) {
        return createNode(renderSupport, index, nodeName, hasMetrics, children, null);
    }

    private static Node createNode(HtmlRenderingSupportImpl renderSupport, int index, String nodeName,
                                   HasMetrics hasMetrics, List<Node> children, String path) {
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
