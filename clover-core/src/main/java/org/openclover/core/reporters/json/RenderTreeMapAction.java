package org.openclover.core.reporters.json;

import org.apache.velocity.VelocityContext;
import org.openclover.core.api.registry.ProjectInfo;
import org.openclover.core.reporters.html.HtmlRenderingSupportImpl;
import org.openclover.core.reporters.html.HtmlReportUtil;

import java.io.File;
import java.util.concurrent.Callable;

public class RenderTreeMapAction implements Callable<Object> {

    private final ProjectInfo project;
    private final File outdir;
    private final VelocityContext mContext;
    private final HtmlRenderingSupportImpl renderSupport = new HtmlRenderingSupportImpl();

    public RenderTreeMapAction(VelocityContext context, File outdir, ProjectInfo project) {
        this.project = project;
        this.outdir = outdir;
        this.mContext = context;
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
        final String jsonStr = RenderTreeMapJsonAction.generateJson(project, renderSupport, classLevel);
        mContext.put("callback", callback);
        mContext.put("json", jsonStr);
        HtmlReportUtil.mergeTemplateToFile(new File(outdir, filename), mContext,
                "treemap-json.vm");
        return jsonStr;
    }

}
