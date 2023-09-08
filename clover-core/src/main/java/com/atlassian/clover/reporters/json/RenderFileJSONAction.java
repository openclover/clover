package com.atlassian.clover.reporters.json;

import clover.org.apache.velocity.VelocityContext;
import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.Logger;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.html.HtmlRenderingSupportImpl;
import com.atlassian.clover.reporters.html.HtmlReportUtil;
import com.atlassian.clover.reporters.html.RenderFileAction;
import com.atlassian.clover.reporters.html.source.SourceRenderHelper;
import com.atlassian.clover.spi.reporters.html.source.LineRenderInfo;
import com.atlassian.clover.util.CloverUtils;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import static org.openclover.util.Lists.newArrayList;

/**
 */
public class RenderFileJSONAction extends RenderFileAction {
    public RenderFileJSONAction(FullFileInfo fileInfo, HtmlRenderingSupportImpl renderingHelper, Current report, VelocityContext velocity, CloverDatabase database, FullProjectInfo fullModel) {
        super(fileInfo, renderingHelper, report, velocity, database, fullModel, null);
    }

    @Override
    public void render() throws Exception {
        final String basename = new File(fileInfo.getName()).getName();

        try {
            final Map<String, Number> columnValues =
                JSONReportUtils.collectColumnValuesFor(columnsTL.get(), fileInfo, renderingHelper);

            final SourceRenderHelper srh = new SourceRenderHelper(database, reportConfig, renderingHelper);
            final FullFileInfo fcopy = fileInfo.copy((FullPackageInfo)fileInfo.getContainingPackage(), HasMetricsFilter.ACCEPT_ALL);
            final LineRenderInfo[] lineInfos = srh.gatherSrcRenderInfo(velocity, fcopy, getContextSet(), "", testLineInfo);

            // the json file used for exporting coverage data - not used by src-file.vm
            final String apiJsonOutFilename = createOutFileBaseName(basename) + "java.js";
            final File apiJsonOutfile = CloverUtils.createOutFile(fcopy, apiJsonOutFilename, reportConfig.getOutFile());

            final Collection<String> hitCounts = collectHitCounts(lineInfos);

            final JSONObject json = new JSONObject();
            json.put("id", renderingHelper.getFileIdentifier(fcopy));
            json.put("stats", columnValues);
            json.put("lines", hitCounts);

            velocity.put("json", json.toString(2));
            velocity.put("callback", reportConfig.getFormat().getCallback());

            HtmlReportUtil.mergeTemplateToFile(apiJsonOutfile, velocity,
                    "api-json.vm");
        } catch (Exception e) {
            Logger.getInstance().error("Invalid Java source found or Clover failed to parse it: " + fileInfo.getPhysicalFile().getAbsolutePath(), e);
        }
    }

    private Collection<String> collectHitCounts(LineRenderInfo[] lineInfos) {
        final Collection<String> hitCounts = newArrayList();
        for (final LineRenderInfo lineInfo : lineInfos) {
            hitCounts.add(lineInfo.getCoverageStr());
        }
        return hitCounts;
    }
}
