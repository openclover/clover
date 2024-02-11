package com.atlassian.clover.reporters.json;

import clover.org.apache.velocity.VelocityContext;
import com.atlassian.clover.reporters.CloverReportConfig;

import com.atlassian.clover.reporters.html.HtmlReportUtil;
import org_openclover_runtime.CloverVersionInfo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

public class RenderColophonJSONAction implements Callable {
    private final VelocityContext ctx;
    private final File file;
    private final CloverReportConfig cfg;

    public RenderColophonJSONAction(VelocityContext ctx, File file, CloverReportConfig cfg) {
        this.ctx = ctx;
        this.file = file;
        this.cfg = cfg;
    }

    @Override
    public Object call() throws Exception {
        file.getParentFile().mkdirs();
        final JSONObject json =
            new JSONObject()
                .put("clover",
                    new JSONObject()
                        .put("release", CloverVersionInfo.RELEASE_NUM)
                        .put("build",
                        new JSONObject()
                            .put("date", CloverVersionInfo.BUILD_DATE)
                            .put("stamp", CloverVersionInfo.BUILD_STAMP)))
                .put("report",
                    new JSONObject()
                        .put("stamp", Long.parseLong(new SimpleDateFormat("yyyyMMddHHmmssSS").format(new Date(System.currentTimeMillis()))))
                        .put("mode", "static")
                        .put("callback", cfg.getFormat().getCallback())
                        .put("columns", selectedColumns()));


        ctx.put("json", json);
        //Statically-served colophon can't be wrapped in a callback or else clients won't be able
        //to determine what the site-wide callback is!
        ctx.put("callback", "");
        HtmlReportUtil.mergeTemplateToFile(file, ctx, "api-json.vm");

        return Void.TYPE;
    }

    private JSONArray selectedColumns() {
        final JSONArray jsonColumns = new JSONArray();
        final List columnNames = JSONReportUtils.getColumnNames(cfg);
        for (Object columnName : columnNames) {
            jsonColumns.put(columnName);
        }
        return jsonColumns;
    }
}
