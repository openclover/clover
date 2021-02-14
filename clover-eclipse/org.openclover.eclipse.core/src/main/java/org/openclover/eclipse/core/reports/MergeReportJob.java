package org.openclover.eclipse.core.reports;

import com.atlassian.clover.CloverDatabase;
import com.atlassian.clover.CloverDatabaseSpec;
import com.atlassian.clover.ProgressListener;
import com.atlassian.clover.cfg.Interval;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import com.atlassian.clover.reporters.CloverReporter;
import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.html.HtmlReporter;
import com.atlassian.clover.reporters.pdf.PDFReporter;
import com.atlassian.clover.reporters.xml.XMLReporter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import java.util.ArrayList;
import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

/**
 *
 */
public class MergeReportJob extends ForkingReportJob {
    private final CloverProject[] projects;

    public MergeReportJob(CloverProject[] projects, Current reportConfig, String vmArgs, String mxSetting) {
        super(reportConfig, vmArgs, mxSetting);
        this.projects = projects;
    }

    @Override
    protected IStatus runReporter(final IProgressMonitor monitor) throws Exception {
        final RuntimeException cancelled = new RuntimeException();
        final CloverDatabaseSpec[] specs = new CloverDatabaseSpec[projects.length];
        monitor.beginTask("Generating report", 100);
        try {
            for (int i = 0; i < projects.length; i++) {
                specs[i] = new CloverDatabaseSpec(projects[i].deriveInitString());
            }
            SubMonitor subMonitor = SubMonitor.convert(monitor, 50);
            subMonitor.beginTask("Merging Clover databases", 100);
            try {
                CloverDatabase.merge(newArrayList(specs), config.getInitString(), new ProgressListener() {
                    private int lastWorked;
                    @Override
                    public void handleProgress(String desc, float pc) {
                        if (monitor.isCanceled()) {
                            throw cancelled;
                        } else {
                            monitor.worked((int)(100 * pc) - lastWorked);
                        }
                        lastWorked = (int)(100 * pc);
                    }
                });
            } finally {
                subMonitor.done();
            }

            subMonitor = SubMonitor.convert(monitor, 50);
            subMonitor.beginTask("Generating report", 100);
            try {
                CloverReporter.buildReporter(config).execute();
            } finally {
                subMonitor.done();
            }
            return Status.OK_STATUS;
        } catch (Exception e) {
            if (e == cancelled) {
                return Status.CANCEL_STATUS;
            } else {
                return new Status(
                    Status.ERROR,
                    CloverPlugin.ID,
                    0,
                    "Clover failed to merge the multiple Clover databases prior to report generation", e);
            }
        } finally {
            monitor.done();
        }
    }

    @Override
    protected String calculateReporterClass() {
        switch (config.getFormat().getType()) {
            case HTML:
                return MergeAndReportHtml.class.getName();
            case PDF:
                return MergeAndReportPdf.class.getName();
            default:
                return MergeAndReportXml.class.getName();
        }
    }

    @Override
    protected String calcTestFilterArgs() {
        return calcTestFilterArgs(projects);
    }

    @Override
    protected String calculateProgramArgs() {
        StringBuilder buf = new StringBuilder();
        for (CloverProject project : projects) {
            buf.append(quote(project.deriveInitString()));
            buf.append(" ");
            //TODO: projects[i].getModel().getDatabase() could be null if db failed to load
            buf.append(quote(project.getSettings().calcEffectiveSpanInterval(project.getModel().getDatabase()).toString()));
            buf.append(" ");
        }
        buf.append("--report ");
        buf.append(super.calculateProgramArgs());
        return buf.toString();
    }

    public abstract static class MergeAndReport extends ForkingReporter {
        private String filterClassName;

        @Override
        protected final int run(String[] args) {
            try {
                String mergedInitFile = null;
                List initStringsAndSpans = new ArrayList(args.length);
                List reportArgs = new ArrayList(args.length);
                List bucket = initStringsAndSpans;
                for (int i = 0; i < args.length; i++) {
                    if (args[i].equals("--report")) {
                        bucket = reportArgs;
                    } else {
                        if ("-i".equals(args[i])) {
                            mergedInitFile = args[i+1];
                        }
                        bucket.add(args[i]);
                    }
                }

                System.out.println(initStringsAndSpans.toString());
                final CloverDatabaseSpec[] specs = new CloverDatabaseSpec[initStringsAndSpans.size() / 2];
                for (int i = 0; i < initStringsAndSpans.size(); i += 2) {
                    specs[i / 2] = new CloverDatabaseSpec((String) initStringsAndSpans.get(i));
                    specs[i / 2].setSpan(new Interval((String) initStringsAndSpans.get(i + 1)));
                }
                CloverDatabase.merge(newArrayList(specs), mergedInitFile, ProgressListener.NOOP_LISTENER);

                //This will probably not return
                return postMergeRun((String[])reportArgs.toArray(new String[reportArgs.size()]));
            } catch (Throwable t) {
                System.out.println("Error generating report: " + t);
                t.printStackTrace(System.out);
                return 1;
            }
        }

        protected abstract int postMergeRun(String[] args);
    }

    public static class MergeAndReportHtml extends MergeAndReport {
        @Override
        protected int postMergeRun(String[] args) {
            return HtmlReporter.runReport(args);
        }
    }

    public static class MergeAndReportPdf extends MergeReportJob.MergeAndReport {
        @Override
        protected int postMergeRun(String[] args) {
            return PDFReporter.runReport(args);
        }
    }
    
    public static class MergeAndReportXml extends MergeAndReport{
        @Override
        protected int postMergeRun(String[] args) {
            return XMLReporter.runReport(args);
        }
    }
}
