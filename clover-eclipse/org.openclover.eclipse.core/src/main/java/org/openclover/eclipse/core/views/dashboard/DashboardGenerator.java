package org.openclover.eclipse.core.views.dashboard;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.openclover.core.CloverDatabase;
import org.openclover.core.reporters.Current;
import org.openclover.core.reporters.html.VelocityLogAdapter;
import org.openclover.core.util.FileUtils;
import org.openclover.runtime.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class DashboardGenerator {
    private static final String DASHBOARD_FILE = "dashboard-eclipse.html";
    private final CloverDatabase database;
    private final File basePath;
    private final File dashboardFile;
    
    private static final String DASHBOARD_VM = "html/dashboard-eclipse.vm";
    static {
        PlainTextVelocityResourceLoader.addPlainTextResource(DASHBOARD_VM);
        PlainTextVelocityResourceLoader.addPlainTextResource("html_res/adg/test-results-bar.vm");
        PlainTextVelocityResourceLoader.addPlainTextResource("html_res/adg/test-noresults-warning.vm");
        PlainTextVelocityResourceLoader.addPlainTextResource("html_res/adg/html-head.vm");
    }

    public DashboardGenerator(CloverDatabase database, File basePath) {
        this.database = database;
        this.basePath = basePath;
        this.dashboardFile = new File(basePath, DASHBOARD_FILE);
    }
    
    public void execute() throws Exception {
        createResources();
        final VelocityContext context = new VelocityContext();

        context.put("baseUrl", getBaseURL());
        context.put("rootRelPath", "");
        context.put("showSrc", Boolean.TRUE);
        context.put("skipCoverageTreeMap", Boolean.TRUE);

        final Current currentConfig = new Current();
        currentConfig.setShowLambdaFunctions(false);
        currentConfig.setShowLambdaFunctions(false);

        new RenderEclipseDashboardAction(context, basePath, database.getAppOnlyModel(), database.getFullModel(),
                null, null, currentConfig).applyCtxChanges();
        
        context.put("renderUtil", new DashboardHtmlRenderingSupport());
        mergePlainTextTemplateToFile(dashboardFile, context, DASHBOARD_VM);
    }
    
    static void mergePlainTextTemplateToFile(File outfile, VelocityContext context, String template) throws Exception {
        final VelocityEngine engine = new VelocityEngine();
        engine.setProperty("resource.loader", "class");
        engine.setProperty("velocimacro.library", "");
        engine.setProperty(
                "class.resource.loader.class",
                PlainTextVelocityResourceLoader.class.getName());
        engine.setProperty("class.resource.loader.cache", "true");
        engine.setProperty("class.resource.loader.modificationCheckInterval", "0");
        engine.setProperty("parser.pool.size", "1");
        engine.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, new VelocityLogAdapter(Logger.getInstance()));
        engine.setProperty("resource.manager.logwhenfound", "false");
        engine.setProperty("runtime.log.invalid.references", "false");
        engine.init();

        mergeTemplateToFile(engine, outfile, context, template);
    }
    
    public String getDashboardURL() {
        try {
            return dashboardFile.toURI().toURL().toExternalForm();
        } catch (MalformedURLException e) {
            return null;
        }
    }
    
    public String getBaseURL() {
        try {
            return basePath.toURI().toURL().toExternalForm();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private void createResources() throws Exception {
        final String templatePath = getTemplatePath();
        copyStaticResource(templatePath, "aui/css/aui.min.css");
        copyStaticResource(templatePath, "aui/css/aui-experimental.min.css");
        copyStaticResource(templatePath, "aui/css/aui-ie9.min.css");
        copyStaticResource(templatePath, "aui/css/atlassian-icons.eot");
        copyStaticResource(templatePath, "aui/css/atlassian-icons.svg");
        copyStaticResource(templatePath, "aui/css/atlassian-icons.ttf");
        copyStaticResource(templatePath, "aui/css/atlassian-icons.woff");

        copyStaticResource(templatePath, "aui/js/aui.min.js");
        copyStaticResource(templatePath, "aui/js/aui-experimental.min.js");
        copyStaticResource(templatePath, "aui/js/aui-soy.min.js");

        copyStaticResource(templatePath, "clover.js");
        copyStaticResource(templatePath, "jquery-1.8.3.min.js");
        copyStaticResource(templatePath, "style.css");
    }

    private void copyStaticResource(final String aLoadPath, final String aName) throws IOException {
        final File outfile = new File(basePath, aName);
        FileUtils.resourceToFile(getClass().getClassLoader(), aLoadPath + "/" + aName, outfile);
    }

    private static void mergeTemplateToFile(VelocityEngine engine, File outfile, VelocityContext context, String template) throws IOException {
        mergeTemplateToStream(engine, Files.newOutputStream(outfile.toPath()), context, template);
    }

    private static void mergeTemplateToStream(VelocityEngine engine, OutputStream outputStream, VelocityContext context, String template) {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            engine.mergeTemplate(template, "ASCII", context, out);
        } catch (Exception e) {
            Logger.getInstance().warn("Failed to generate " + outputStream, e);
        }
    }

    // see HtmlReportUtil#getTemplatePath()
    private static String getTemplatePath() {
        return "html_res/adg";
    }
}
