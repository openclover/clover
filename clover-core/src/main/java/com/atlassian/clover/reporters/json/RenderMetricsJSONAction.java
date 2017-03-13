package com.atlassian.clover.reporters.json;

import clover.org.apache.velocity.VelocityContext;
import com.atlassian.clover.api.registry.FileInfo;
import com.atlassian.clover.api.registry.PackageInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.reporters.CloverReportConfig;
import com.atlassian.clover.reporters.Columns;
import com.atlassian.clover.reporters.html.HtmlRenderingSupportImpl;
import com.atlassian.clover.reporters.html.HtmlReportUtil;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.entities.FullProjectInfo;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static clover.com.google.common.collect.Lists.newArrayList;

public class RenderMetricsJSONAction implements Callable {
    private static ThreadLocal columns;

    private final HasMetrics mInfo;
    private final File mFile;
    private final HtmlRenderingSupportImpl mHelper;
    private final CloverReportConfig mCfg;
    private final VelocityContext mCtx;

    public RenderMetricsJSONAction(
        VelocityContext ctx, HasMetrics configured,
        CloverReportConfig current, File outfile, HtmlRenderingSupportImpl helper) {
        mInfo = configured;
        mCtx = ctx;
        mHelper = helper;
        mCfg = current;
        mFile = outfile;
    }

    /**
     * Initialises all thread locals.
     * This is to be called once before the {@link #call} method .
     */
    public static void initThreadLocals() {
        columns = new ThreadLocal();
    }

    /**
     * Resets all thread locals.
     * This is to be called once all files have been rendered.
     * NB: {@link ThreadLocal#remove} can't be used since it is since jdk 1.5 .
     */
    public static void resetThreadLocals() {
        columns = null;
    }

    @Override
    public Object call() throws Exception {
        //First action to be called per-thread sets up the TLS columns and contextset
        if (columns.get() == null) {
            final List cols = mCfg.isColumnsSet() ? mCfg.getColumns().getProjectColumnsCopy() : Columns.getAllColumns();
            columns.set(cols);
        }
        render();
        return null;
    }

    public void render() throws Exception {
        final Map<String, Number> columnValues =
            JSONReportUtils.collectColumnValuesFor((List)columns.get(), mInfo, mHelper);
        
        final JSONObject json =
            new JSONObject()
                .put("name", mInfo.getName())
                .put("title", mCfg.getTitle())
                .put("stats", columnValues);

        // put the list of package names in here too
        final List<String> children = newArrayList();
        if (mInfo instanceof FullProjectInfo) {// TODO: children should be passed into the constructor
            final FullProjectInfo projectInfo = (FullProjectInfo)mInfo;
            final List<? extends PackageInfo> pkgs = projectInfo.getAllPackages();
            for (PackageInfo pkg : pkgs) {
                children.add(pkg.getPath());
            }
        } else if (mInfo instanceof FullPackageInfo) {
            final List<? extends FileInfo> files = ((FullPackageInfo)mInfo).getFiles();
            for (final FileInfo file : files) {
                children.add(file.getName());
            }
        }
        json.put("children", children);

        mCtx.put("json", json);
        mCtx.put("callback", mCfg.getFormat().getCallback());
        HtmlReportUtil.mergeTemplateToFile(mFile, mCtx, "api-json.vm");
    }
}
