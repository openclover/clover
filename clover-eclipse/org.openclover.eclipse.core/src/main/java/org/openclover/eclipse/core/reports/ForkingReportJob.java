package org.openclover.eclipse.core.reports;

import com.atlassian.clover.reporters.Current;
import com.atlassian.clover.reporters.TestSelectionHelper;
import org.openclover.eclipse.core.CloverPlugin;
import org.openclover.eclipse.core.projects.CloverProject;
import org.openclover.eclipse.core.projects.settings.ProjectSettings;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import static org.openclover.util.Lists.newArrayList;

public abstract class ForkingReportJob extends ReportJob {
    private static final long POLL_INTERVAL = 1000;
    private static final long MAX_REPORT_DURATION = 1000 * 60 * 60;

    protected final Current config;
    protected String mxSetting;
    protected String vmArgs;

    public ForkingReportJob(Current config, String vmArgs, String mxSetting) {
        this.config = config;
        this.vmArgs = vmArgs;
        this.mxSetting = mxSetting;
    }

    @Override
    public Current getConfig() {
        return config;
    }


    protected ILaunchConfigurationWorkingCopy launchConfigFor(String name) throws CoreException {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType launchType =
            manager.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
        ILaunchConfiguration[] launchConfig =
            manager.getLaunchConfigurations(launchType);
        ILaunchConfigurationWorkingCopy launchConfigCopy =
            launchType.newInstance(null, name);
        return launchConfigCopy;
    }

    protected URL calculateCorePluginJarURL() throws IOException {
        URL corePlugin = FileLocator.resolve(
            FileLocator.find(Platform.getBundle(CloverPlugin.ID),
            new Path("/"),
            null));
        URL corePluginJar = new URL(corePlugin.getPath().replaceAll("!/", ""));

        return corePluginJar;
    }

    protected void bindCloverRequiredJvm(ILaunchConfigurationWorkingCopy launchConfigCopy) {
        IVMInstall jre = JavaRuntime.getDefaultVMInstall();
        launchConfigCopy.setAttribute(
            IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
            jre.getInstallLocation().getAbsolutePath());
    }

    protected static String blankIfNull(String string) {
        return string == null ? "" : string;
    }

    protected static String quote(String string) {
        return "\"" + string.replaceAll("\"", "\\\"") + "\"";
    }

    protected void bindReportingSystemProperties(ILaunchConfigurationWorkingCopy launchConfigCopy) {
        launchConfigCopy.setAttribute(
            IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
            userArgs()
            + " -Djava.awt.headless=true"
            + " -D" + ForkingReporter.FORKING_REPORTER_PROP + "=" + calculateReporterClass());
    }

    private String userArgs() {
        return
            (((vmArgs.contains("-Xmx")) ? "" : ("-Xmx" + mxSetting + " ")) + vmArgs) + " ";
    }

    protected void bindCloverRequiredClasspath(ILaunchConfigurationWorkingCopy launchConfigCopy) throws IOException, CoreException {
        URL corePluginJar = calculateCorePluginJarURL();

        launchConfigCopy.setAttribute(
                IJavaLaunchConfigurationConstants.ATTR_CLASSPATH,
                newArrayList(
                        userClasspathEntryFor(corePluginJar).getMemento(),
                        getJdkClassEntry().getMemento())
        );

        launchConfigCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);
    }

    private IRuntimeClasspathEntry getJdkClassEntry() throws CoreException {
        IPath systemLibsPath = new Path(JavaRuntime.JRE_CONTAINER);
        IRuntimeClasspathEntry systemLibsEntry =
            JavaRuntime.newRuntimeContainerClasspathEntry(systemLibsPath,
            IRuntimeClasspathEntry.STANDARD_CLASSES);
        return systemLibsEntry;
    }

    private IRuntimeClasspathEntry userClasspathEntryFor(URL jarUrl) {
        IRuntimeClasspathEntry cloverCorePluginPath =
            JavaRuntime.newArchiveRuntimeClasspathEntry(new Path(jarUrl.getPath()));
        cloverCorePluginPath.setClasspathProperty(IRuntimeClasspathEntry.USER_CLASSES);
        return cloverCorePluginPath;
    }

    protected void bindMainClassArguments(ILaunchConfigurationWorkingCopy launchConfigCopy, String mainClassArgs) {
        launchConfigCopy.setAttribute(
            IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
            mainClassArgs);
    }

    protected void bindMainClassName(ILaunchConfigurationWorkingCopy launchConfigCopy, String mainClassName) {
        launchConfigCopy.setAttribute(
            IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
            mainClassName);
    }

    protected IStatus report(IProgressMonitor monitor)  {
        try {
            ILaunch launch = buildAndLaunchForkedReporter(monitor);
            long started = System.currentTimeMillis();
            while (!launch.isTerminated()
                   && !reportTakingTooLong(started)) {
                try {
                    Thread.sleep(POLL_INTERVAL);
                } catch (InterruptedException e) {
                    //Ignore
                }
                if (monitor.isCanceled()) {
                    launch.terminate();
                    return Status.CANCEL_STATUS;
                }
            }

            int launchExitValue = getLaunchExitValue(launch);
            if (launchExitValue != 0) {
                return new Status(
                    Status.ERROR,
                    CloverPlugin.ID,
                    0,
                    "The JVM report process failed with error code " + launchExitValue + " - see log for details",
                    null);
            } else {
                return Status.OK_STATUS;
            }
        } catch (Throwable e) {
            return new Status(Status.ERROR, CloverPlugin.ID, 0, "Clover failed to generate the report", e);
        }
    }

    protected abstract IStatus runReporter(IProgressMonitor monitor) throws Exception;

    private int getLaunchExitValue(ILaunch launch) throws DebugException {
        IProcess[] processes = launch.getProcesses();
        if (processes.length != 1) {
            return -1;
        } else {
            return processes[0].getExitValue();
        }
    }

    private boolean reportTakingTooLong(long started) {
        return System.currentTimeMillis() - started > MAX_REPORT_DURATION;
    }

    protected ILaunch buildAndLaunchForkedReporter(IProgressMonitor monitor) throws Exception {
        ILaunchConfigurationWorkingCopy launchConfigCopy = launchConfigFor("Clover Reports");

        bindCloverRequiredClasspath(launchConfigCopy);
        bindCloverRequiredJvm(launchConfigCopy);
        bindReportingSystemProperties(launchConfigCopy);
        bindMainClassName(launchConfigCopy, ForkingReporter.class.getName());
        bindMainClassArguments(launchConfigCopy, calculateProgramArgs());

        CloverPlugin.logVerbose(
            "Launching report VM with args: " + launchConfigCopy.getAttributes().toString());

        launchConfigCopy.doSave();

        return launchConfigCopy.launch(ILaunchManager.RUN_MODE, monitor);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Generating report", 100);
        try {
            return report(monitor);
        } finally {
            monitor.done();
        }
    }

    protected String calculateProgramArgs() {
        final String testFilterArgs = calcTestFilterArgs();
        switch (config.getFormat().getType()) {
            case HTML:
                return testFilterArgs + " " + calcHtmlReportArgs();
            case PDF:
                return testFilterArgs + " " + calcPdfReportArgs();
            default:
                return testFilterArgs + " " + calcXmlReportArgs();
        }
    }

    protected abstract String calculateReporterClass();

    protected abstract String calcTestFilterArgs();

    protected String calcHtmlReportArgs() {
        return "-a -i " + quote(config.getInitString())
        + " -o " + quote(config.getOutFile().getAbsolutePath())
        + " -t " + quote(config.getTitle())
        + " -tc " + config.getNumThreads()
        + (config.getFormat().getSrcLevel() ? "" : " -h ")
        + (config.isIncludeFailedTestCoverage() ? " -if " : "")
        + (config.isShowLambdaFunctions() ? " -sl " : "")
        + (config.isShowInnerFunctions() ? " -si " : "")
        + " -f " + quote(blankIfNull(config.getFormat().getFilter()))
        + " -s " + quote(config.getSpan().toString());
    }

    protected String calcPdfReportArgs() {
        return "-a -i " + quote(config.getInitString())
        + " -o " + quote(config.getOutFile().getAbsolutePath())
        + " -t " + quote(config.getTitle())
        + " -tc " + config.getNumThreads()
        + (config.isIncludeFailedTestCoverage() ? " -if " : "")
        + " -f " + quote(blankIfNull(config.getFormat().getFilter()))
        + " -s " + quote(config.getSpan().toString());
    }

    protected String calcXmlReportArgs() {
        return "-a -i " + quote(config.getInitString())
        + " -o " + quote(config.getOutFile().getAbsolutePath())
        + " -t " + quote(config.getTitle())
        + " -tc " + config.getNumThreads()
        + (config.getFormat().getSrcLevel() ? " -l " : "")
        + (config.isIncludeFailedTestCoverage() ? " -if " : "")
        + (config.isShowLambdaFunctions() ? " -sl " : "")
        + (config.isShowInnerFunctions() ? " -si " : "")
        + " -f " + quote(blankIfNull(config.getFormat().getFilter()))
        + " -s " + quote(config.getSpan().toString());
    }

    public static void extractTestFilterPatterns(CloverProject project, Collection<String> includePatterns, Collection<String> excludePatterns) {
        final String projectBase = project.getProject().getLocation().toString() + "/";
        final ProjectSettings settings = project.getSettings();
        switch (settings.getTestSourceFolders()) {
            case ProjectSettings.Values.SELECTED_FOLDERS:
                final List<String> selectedFolders = settings.getSelectedTestFolders();
                for (String selectedFolder : selectedFolders) {
                    includePatterns.add(projectBase + selectedFolder + "/**/*.java");
                }
                break;
            case ProjectSettings.Values.ALL_FOLDERS:
                for (String s : settings.calculateTestIncludeFilter()) {
                    includePatterns.add(projectBase + s);
                }
                for (String s : settings.calculateTestExcludeFilter()) {
                    excludePatterns.add(projectBase + s);
                }
                break;
            default:
                excludePatterns.add(projectBase + "**/*.*");
        }
    }

    protected void addParamOptionally(StringBuilder sb, String param, List<String> args) {
        if (args.size() > 0) {
            String sep = "";
            final StringBuilder patterns = new StringBuilder();
            for (String includePattern : args) {
                patterns.append(sep);
                patterns.append(includePattern);
                sep = ", ";
            }
            if (sb.length() == 0) {
                sb.append(" ");
            }
            sb.append(param).append(" ");
            sb.append(quote(patterns.toString()));
        }
    }

    protected String calcTestFilterArgs(CloverProject... projects) {
        final ArrayList<String> includePatterns = newArrayList();
        final ArrayList<String> excludePatterns = newArrayList();

        for (CloverProject project : projects) {
            extractTestFilterPatterns(project, includePatterns, excludePatterns);
        }
        StringBuilder sb = new StringBuilder();
        addParamOptionally(sb, TestSelectionHelper.TESTS_INCLUDE_PATTERN_PARAM, includePatterns);
        addParamOptionally(sb, TestSelectionHelper.TESTS_EXCLUDE_PATTERN_PARAM, excludePatterns);
        return sb.toString();
    }
}
