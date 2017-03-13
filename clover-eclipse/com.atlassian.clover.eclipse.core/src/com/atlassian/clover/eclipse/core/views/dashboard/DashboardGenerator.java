package com.atlassian.clover.eclipse.core.views.dashboard;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import clover.org.apache.velocity.VelocityContext;
import clover.org.apache.velocity.app.Velocity;
import clover.org.apache.velocity.app.VelocityEngine;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.Logger;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.html.HtmlReportUtil;
import com.atlassian.clover.reporters.html.VelocityLogAdapter;
import com.atlassian.clover.util.FileUtils;

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

        HtmlReportUtil.mergeTemplateToFile(engine, outfile, context, template);
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
        final String templatePath = HtmlReportUtil.getTemplatePath();
        copyStaticResource(templatePath, "aui/css/aui.min.css");
        copyStaticResource(templatePath, "aui/css/aui-experimental.min.css");
        copyStaticResource(templatePath, "aui/css/aui-ie9.min.css");
        copyStaticResource(templatePath, "aui/css/atlassian-icons.eot");
        copyStaticResource(templatePath, "aui/css/atlassian-icons.svg");
        copyStaticResource(templatePath, "aui/css/atlassian-icons.ttf");
        copyStaticResource(templatePath, "aui/css/atlassian-icons.woff");

        copyStaticResource(templatePath, "aui/js/aui.min.js");
        copyStaticResource(templatePath, "aui/js/aui-datepicker.min.js");
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

}
