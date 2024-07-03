package org.openclover.core.reporters.html;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.reporters.Column;
import org.openclover.core.reporters.ColumnFormat;
import org.openclover.core.reporters.Columns;
import org.openclover.runtime.Logger;
import org.openclover.runtime.util.Formatting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class HtmlReportUtil {
    private static final ThreadLocal<VelocityEngine> ve = ThreadLocal.withInitial(() -> HtmlReportUtil.newVelocityEngine(true));

    public static VelocityEngine getVelocityEngine() {
        return ve.get();
    }

    public static VelocityEngine newVelocityEngine(boolean withClasspathLoader) {
        VelocityEngine engine = new VelocityEngine();
        try {
            engine.setProperty("resource.loader", "class");
            engine.setProperty("velocimacro.library", "");
            engine.setProperty(
                    "class.resource.loader.class",
                    withClasspathLoader
                            ? ClasspathResourceLoader.class.getName()
                            : PlainTextVelocityResourceLoader.class.getName());
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
     * containing {@link Columns}.**/
    static final int EXTRA_COLS = 1;

    public static void mergeTemplateToFile(VelocityEngine engine, File outfile,
                                           VelocityContextBuilder context, String template) throws IOException {
        mergeTemplateToStream(engine, Files.newOutputStream(outfile.toPath()), context, template);
    }

    public static void mergeTemplateToStream(VelocityEngine engine, OutputStream outputStream,
                                             VelocityContextBuilder context, String template) {

        if (Logger.isDebug())
            Logger.getInstance().debug("rendering " + template);

        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            if (engine.mergeTemplate(template, "ASCII", context.build(), out)) {
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

    public static void mergeTemplateToFile(File outfile, VelocityContextBuilder context, String template) throws IOException {
        mergeTemplateToFile(getVelocityEngine(), outfile, context, getTemplatePath(template));
    }

    public static void mergeTemplateToStream(OutputStream outputStream, VelocityContextBuilder context, String template) throws IOException {
        mergeTemplateToStream(getVelocityEngine(), outputStream, context, getTemplatePath(template));
    }

    public static void mergeTemplateToDir(File basePath, String templateName, VelocityContextBuilder context) throws IOException {
        File outfile = new File(basePath, templateName);
        context.put("currentPageURL", templateName);
        mergeTemplateToFile(outfile, context, templateName);
    }


    public static void addColumnsToContext(VelocityContextBuilder context, List<Column> cols, HasMetrics parent, List<? extends HasMetrics> children) {
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
    
    public static void addFilteredPercentageToContext(VelocityContextBuilder context, HasMetrics model) {
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