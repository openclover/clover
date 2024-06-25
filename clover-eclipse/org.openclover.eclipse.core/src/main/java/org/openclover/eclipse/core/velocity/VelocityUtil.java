package org.openclover.eclipse.core.velocity;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.openclover.eclipse.core.views.dashboard.PlainTextVelocityResourceLoader;
import org.openclover.runtime.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Similar to HtmlReportUtil but it uses non-shaded Velocity.
 */
public class VelocityUtil {

    private static final ThreadLocal<VelocityEngine> ve = ThreadLocal.withInitial(VelocityUtil::createVelocityEngine);

    public static VelocityEngine getVelocityEngine() {
        return ve.get();
    }

    private static VelocityEngine createVelocityEngine() {
        final VelocityEngine engine = new VelocityEngine();
        engine.setProperty("resource.loader", "class");
        engine.setProperty("velocimacro.library", "");
        engine.setProperty(
                "class.resource.loader.class",
                PlainTextVelocityResourceLoader.class.getName());
        engine.setProperty("class.resource.loader.cache", "true");
        engine.setProperty("class.resource.loader.modificationCheckInterval", "0");
        engine.setProperty("parser.pool.size", "1");
        engine.setProperty("resource.manager.logwhenfound", "false");
        engine.setProperty("runtime.log.invalid.references", "false");
        engine.init();
        return engine;
    }

    public static String getTemplatePath() {
        return "html_res/adg";
    }

    public static void mergeTemplateToDir(VelocityEngine engine, File basePath, String templateName, VelocityContext context) throws IOException {
        File outfile = new File(basePath, templateName);
        context.put("currentPageURL", templateName);
        mergeTemplateToFile(engine, outfile, context, templateName);
    }

    public static void mergeTemplateToFile(VelocityEngine engine, File outfile, VelocityContext context, String template) throws IOException {
        mergeTemplateToStream(engine, Files.newOutputStream(outfile.toPath()), context, template);
    }

    public static void mergeTemplateToStream(VelocityEngine engine, OutputStream outputStream, VelocityContext context, String template) {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            engine.mergeTemplate(template, "ASCII", context, out);
        } catch (Exception e) {
            Logger.getInstance().warn("Failed to generate " + outputStream, e);
        }
    }
}
