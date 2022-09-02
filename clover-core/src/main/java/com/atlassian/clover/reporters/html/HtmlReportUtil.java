package com.atlassian.clover.reporters.html;

import clover.org.apache.velocity.VelocityContext;
import clover.org.apache.velocity.app.Velocity;
import clover.org.apache.velocity.app.VelocityEngine;
import com.atlassian.clover.Logger;
import com.atlassian.clover.reporters.Column;
import com.atlassian.clover.reporters.ColumnFormat;
import com.atlassian.clover.util.Formatting;
import com.atlassian.clover.api.registry.HasMetrics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class HtmlReportUtil {
    private static ThreadLocal ve = new ThreadLocal() {
        @Override
        protected Object initialValue() {
            return newVelocityEngine();
        }
    };

    public static VelocityEngine getVelocityEngine() {
        return (VelocityEngine) ve.get();
    }

    static VelocityEngine newVelocityEngine() {
        VelocityEngine engine = new VelocityEngine();
        try {
            engine.setProperty("resource.loader", "class");
            engine.setProperty("velocimacro.library", "");
            engine.setProperty(
                    "class.resource.loader.class",
                    clover.org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader.class.getName());
            engine.setProperty("class.resource.loader.cache", "true");
            engine.setProperty("class.resource.loader.modificationCheckInterval", "0");
            engine.setProperty("parser.pool.size", "1");
            engine.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, new VelocityLogAdapter(Logger.getInstance()));
            engine.setProperty("resource.manager.logwhenfound", "false");
            engine.setProperty("runtime.log.invalid.references", "false");
            engine.init();
        } catch (Exception e) {
            Logger.getInstance().error("Could not load templating engine. " + e.getMessage(), e);
            return null;
        }
        return engine;
    }


    /** The number of extra columns used when rendering tables
     * containing {@link com.atlassian.clover.reporters.Columns}.**/
    static final int EXTRA_COLS = 1;

    public static void mergeTemplateToFile(VelocityEngine engine, File outfile, VelocityContext context, String template) throws IOException {
        mergeTemplateToStream(engine, new FileOutputStream(outfile), context, template);
    }

    public static void mergeTemplateToStream(VelocityEngine engine, OutputStream outputStream, VelocityContext context, String template) throws IOException {

        if (Logger.isDebug())
            Logger.getInstance().debug("rendering " + template);

        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"))) {
            if (engine.mergeTemplate(template, "ASCII", context, out)) {
                if (Logger.isDebug()) {
                    Logger.getInstance().debug("done ");
                }
            } else {
                if (Logger.isDebug()) {
                    Logger.getInstance().warn("Failed to generate ");
                }
            }
        } catch (Exception e) {
            Logger.getInstance().warn("Failed to generate " + outputStream, e);
        }

    }

    public static void mergeTemplateToFile(File outfile, VelocityContext context, String template) throws IOException {
        mergeTemplateToFile(getVelocityEngine(), outfile, context, getTemplatePath(template));
    }

    public static void mergeTemplateToStream(OutputStream outputStream, VelocityContext context, String template) throws IOException {
        mergeTemplateToStream(getVelocityEngine(), outputStream, context, getTemplatePath(template));
    }

    public static void mergeTemplateToDir(File basePath, String templateName, VelocityContext context) throws IOException {
        File outfile = new File(basePath, templateName);
        context.put("currentPageURL", templateName);
        mergeTemplateToFile(outfile, context, templateName);
    }


    public static void addColumnsToContext(VelocityContext context, List<Column> cols, HasMetrics parent, List<? extends HasMetrics> children) {
        HasMetrics childInfo = children != null && children.size() > 0 ? children.get(0) : null;
        context.put("columns", cols);
        int colSpan = EXTRA_COLS;
        for (Column column : cols) {
            ColumnFormat format = column.getFormat();
            colSpan += format.getColSpan();
        }
        context.put("colSpan", colSpan);
        context.put("headerInfo", parent);
        context.put("childHeaderInfo", childInfo);

    }
    
    public static void addFilteredPercentageToContext(VelocityContext context, HasMetrics model) {
        float pcFiltered = getPercentageFiltered(model);
        if (pcFiltered > 0) {
            String percentFiltered = Formatting.getPercentStr(pcFiltered);
            context.put("percentFiltered", percentFiltered);
            context.put("showFilterToggle", hasFilteredMetrics(model));
        }
    }

    public static boolean hasFilteredMetrics(HasMetrics model) {
        return model.getMetrics().getNumElements() != model.getRawMetrics().getNumElements();
    }

    public static float getPercentageFiltered(HasMetrics model) {
        float rawElements = model.getRawMetrics().getNumElements();
        if (rawElements > 0) {
            final int numElements = model.getMetrics().getNumElements();
            return (1.0f - (numElements / rawElements));
        }
        return -1.0f;
    }

    public static String getTemplatePath() {
        return "html_res/adg";
    }

    public static String getTemplatePath(String template) {
        return getTemplatePath() + "/" + template;
    }
}