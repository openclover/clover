package com.atlassian.clover.reporters;

import clover.org.apache.velocity.VelocityContext;
import com.atlassian.clover.registry.entities.BaseClassInfo;
import com.atlassian.clover.registry.metrics.HasMetricsSupport;
import com.atlassian.clover.reporters.html.ClassInfoStatsCalculator;
import com.atlassian.clover.reporters.html.HtmlReportUtil;
import com.atlassian.clover.reporters.html.StatisticsClassInfoVisitor;
import com.atlassian.clover.spi.reporters.html.source.HtmlRenderingSupport;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class CloudGenerator {

    private static final String RENDER_UTIL = "renderUtil";
    private static final String DEEPAXIS = "deepaxis";
    private static final String DEEPAXIS_1 = "deepaxis1";
    private static final String DEEPAXIS_2 = "deepaxis2";
    private static final String SHOW_SRC = "showSrc";


    protected final String template;
    protected final HtmlRenderingSupport axisRendering;
    protected final OutputStream outputStream;
    protected final VelocityContext velocityContext;

    public CloudGenerator(String template, HtmlRenderingSupport axisRendering, OutputStream outputStream) {
        this(template, axisRendering, outputStream, new VelocityContext());
    }

    public CloudGenerator(String template, HtmlRenderingSupport axisRendering, OutputStream outputStream, VelocityContext velocityContext) {
        this.template = template;
        this.axisRendering = axisRendering;
        this.outputStream = outputStream;
        this.velocityContext = velocityContext;
    }

    public final void createReport(List classes,
                                   ClassInfoStatsCalculator calcAxis1,
                                   ClassInfoStatsCalculator calcAxis2) throws IOException {
        applyAxis(classes, calcAxis1, calcAxis2, velocityContext);
        velocityContext.put(RENDER_UTIL, axisRendering);
        velocityContext.put(SHOW_SRC, true);
        HtmlReportUtil.mergeTemplateToStream(outputStream, velocityContext,
                template);
    }

    protected final void applyAxis(List classes, ClassInfoStatsCalculator axis1, ClassInfoStatsCalculator axis2, VelocityContext context) {
        StatisticsClassInfoVisitor v2 = StatisticsClassInfoVisitor.visit(sort(classes), axis2);
        StatisticsClassInfoVisitor v1 = StatisticsClassInfoVisitor.visit(v2.getClasses(), axis1);
        context.put(DEEPAXIS, Boolean.TRUE);
        context.put(DEEPAXIS_1, v1);
        context.put(DEEPAXIS_2, v2);
    }

    protected final List<BaseClassInfo> sort(List<BaseClassInfo> classes) {
        classes.sort(HasMetricsSupport.CMP_LEX);
        return classes;
    }

}
