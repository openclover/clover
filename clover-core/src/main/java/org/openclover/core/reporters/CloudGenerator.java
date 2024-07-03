package org.openclover.core.reporters;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.registry.metrics.HasMetricsSupport;
import org.openclover.core.reporters.html.ClassInfoStatsCalculator;
import org.openclover.core.reporters.html.HtmlReportUtil;
import org.openclover.core.reporters.html.StatisticsClassInfoVisitor;
import org.openclover.core.reporters.html.VelocityContextBuilder;
import org.openclover.core.spi.reporters.html.source.HtmlRenderingSupport;

import java.io.IOException;
import java.io.OutputStream;
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
    protected final VelocityContextBuilder velocityContext;

    public CloudGenerator(String template, HtmlRenderingSupport axisRendering, OutputStream outputStream) {
        this(template, axisRendering, outputStream, VelocityContextBuilder.create());
    }

    public CloudGenerator(String template, HtmlRenderingSupport axisRendering, OutputStream outputStream,
                          VelocityContextBuilder velocityContext) {
        this.template = template;
        this.axisRendering = axisRendering;
        this.outputStream = outputStream;
        this.velocityContext = velocityContext;
    }

    public final void createReport(List<ClassInfo> classes,
                                   ClassInfoStatsCalculator calcAxis1,
                                   ClassInfoStatsCalculator calcAxis2) throws IOException {
        applyAxis(classes, calcAxis1, calcAxis2, velocityContext);
        velocityContext.put(RENDER_UTIL, axisRendering);
        velocityContext.put(SHOW_SRC, true);
        HtmlReportUtil.mergeTemplateToStream(outputStream, velocityContext,
                template);
    }

    protected final void applyAxis(List<ClassInfo> classes,
                                   ClassInfoStatsCalculator axis1,
                                   ClassInfoStatsCalculator axis2,
                                   VelocityContextBuilder context) {
        StatisticsClassInfoVisitor v2 = StatisticsClassInfoVisitor.visit(sort(classes), axis2);
        StatisticsClassInfoVisitor v1 = StatisticsClassInfoVisitor.visit(v2.getClasses(), axis1);
        context.put(DEEPAXIS, Boolean.TRUE);
        context.put(DEEPAXIS_1, v1);
        context.put(DEEPAXIS_2, v2);
    }

    protected final <T extends ClassInfo> List<T> sort(List<T> classes) {
        classes.sort(HasMetricsSupport.CMP_LEX);
        return classes;
    }

}
