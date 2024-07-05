package org.openclover.eclipse.core.views.dashboard;

import org.openclover.core.CloverDatabase;
import org.openclover.core.reporters.Current;
import org.openclover.core.reporters.html.HtmlReportUtil;
import org.openclover.core.reporters.html.PlainTextVelocityResourceLoader;
import org.openclover.core.reporters.html.VelocityContextBuilder;
import org.openclover.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

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
        final VelocityContextBuilder context = VelocityContextBuilder.create()
                .put("baseUrl", getBaseURL())
                .put("rootRelPath", "")
                .put("showSrc", Boolean.TRUE)
                .put("skipCoverageTreeMap", Boolean.TRUE);

        final Current currentConfig = new Current();
        currentConfig.setShowLambdaFunctions(false);
        currentConfig.setShowLambdaFunctions(false);

        new RenderEclipseDashboardAction(context, basePath, database.getAppOnlyModel(), database.getFullModel(),
                null, null, currentConfig).applyCtxChanges();
        
        context.put("renderUtil", new DashboardHtmlRenderingSupport());
        mergePlainTextTemplateToFile(dashboardFile, context, DASHBOARD_VM);
    }
    
    static void mergePlainTextTemplateToFile(File outfile, VelocityContextBuilder context, String template) throws Exception {
        HtmlReportUtil.mergeTemplateToFile(HtmlReportUtil.newVelocityEngine(false), outfile, context, template);
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
