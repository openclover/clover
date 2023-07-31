package org.openclover.ci;

import com.atlassian.clover.ant.tasks.AntInstrumentationConfig;
import com.atlassian.clover.ant.tasks.CloverEnvTask;
import org.openclover.util.ClassPathUtil;
import com.atlassian.clover.api.optimization.Optimizable;
import com.atlassian.clover.optimization.LocalSnapshotOptimizer;
import com.atlassian.clover.optimization.Snapshot;
import com.atlassian.clover.api.optimization.OptimizationOptions;
import com.atlassian.clover.Logger;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.CloverNames;

import java.util.Vector;
import java.util.List;
import java.util.Enumeration;
import java.util.Map;
import java.util.Arrays;
import java.lang.reflect.Field;
import java.io.IOException;

import com.atlassian.clover.ant.taskdefs.CloverCompilerAdapter;

import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.Javadoc;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTask;
import org.apache.tools.ant.taskdefs.optional.junit.BatchTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import static org.openclover.util.Lists.newArrayList;

public class AntIntegrationListener implements BuildListener {
    private OptimizationOptions optimizationOptions;
    private boolean instrumentationOccured;
    private boolean executionOccured;
    private boolean importOccured;

    @Override
    public void buildStarted(BuildEvent buildEvent) {
        AntInstrumentationConfig cfg = getConfigForProject(buildEvent.getProject());
        optimizationOptions = new OptimizationOptions.Builder().initStringAndSnapshotFrom(cfg.resolveInitString()).build();
    }

    @Override
    public void buildFinished(BuildEvent buildEvent) {

        final StringBuffer msg = new StringBuffer("[clover]");
        if (!instrumentationOccured && !executionOccured) {
            msg.append(" No Clover reports written. ");
            appendReason(msg);
            buildEvent.getProject().log(msg.toString());
            return;
        }

        if (isOptimizationEnabled()) {
            createSnapshot();
        }
        
        if (executionOccured && instrumentationOccured && importOccured) {
            Vector<String> targets = new Vector<>(Arrays.asList("clover.current", "clover.json", "clover.report"));
            buildEvent.getProject().executeTargets(targets);
        } else {
            msg.append(" Clover reports not being generated.");
            appendReason(msg);
            buildEvent.getProject().log(msg.toString());
        }
    }

    private void appendReason(StringBuffer msg) {
        if (!instrumentationOccured) {
            msg.append(" No Clover instrumentation was done.");
        }
        if (!executionOccured) {
            msg.append(" No tests were run.");
        }
        if (!importOccured) {
            msg.append(" Clover targets could not be imported into this project.");
        }        
    }

    private void createSnapshot() {
        try {
            Snapshot.generateFor(optimizationOptions.getInitString()).store();

        } catch (IOException | CloverException e) {
            Logger.getInstance().debug("Exception when writing snapshot", e);
            Logger.getInstance().error("Problem writing snapshot file: " + e.getMessage());
        }
    }

    @Override
    public void targetStarted(BuildEvent buildEvent) {

    }

    @Override
    public void targetFinished(BuildEvent buildEvent) {
    }

    @Override
    public void taskStarted(BuildEvent buildEvent) {

        if (!importOccured) {
            importOccured = true;
            setSystemProperties(buildEvent);
            importCloverTargets(buildEvent);
        }

        Task task = buildEvent.getTask();

        if (Logger.isDebug()) {
            Logger.getInstance().debug("Started task: " + task.getTaskName());
        }

        Object maybeTask = getConfiguredTask(task);
        if (maybeTask instanceof Task) {
            Task configuredTask = (Task) maybeTask;

            final String name = configuredTask.getClass().getSimpleName();
            if (Logger.isDebug()) {
                Logger.getInstance().debug("Started task class.getSimpleName(): " + name);
            }
            if ("javac".equalsIgnoreCase(name)) {
                injectClover((Javac) configuredTask);
            } else if ("junittask".equalsIgnoreCase(name)) {
                injectClover((JUnitTask) configuredTask, isOptimizationEnabled());
            } else if ("java".equalsIgnoreCase(name)) {
                injectClover((Java) configuredTask);
            } else if ("javadoc".equalsIgnoreCase(name)) {
                injectClover((Javadoc) configuredTask);
            }
        }
    }


    private boolean isOptimizationEnabled() {
        return Boolean.getBoolean(getCloverOptimizeProperty());
    }

    public String getCloverOptimizeProperty() {
        return CloverNames.PROP_CLOVER_OPTIMIZATION_ENABLED;
    }

    private void importCloverTargets(BuildEvent buildEvent) {
        CloverEnvTask envTask = new CloverEnvTask();
        envTask.setProject(buildEvent.getProject());
        envTask.setTaskName("clover-env");
        envTask.init();
        envTask.execute();
    }

    private void setSystemProperties(BuildEvent buildEvent) {
        // pass all -Dclover. properties as System Properties
        final Map<String, Object> cloverProps = buildEvent.getProject().getProperties();
        for (Map.Entry<String, Object> entry : cloverProps.entrySet()) {
            if (entry.getKey().startsWith(CloverNames.PROP_PREFIX)) {
                System.setProperty(entry.getKey(), entry.getValue().toString());
            }
        }
    }


    @Override
    public void taskFinished(BuildEvent buildEvent) {
    }

    @Override
    public void messageLogged(BuildEvent buildEvent) {
    }

    private AntInstrumentationConfig getConfigForProject(Project project) {
        AntInstrumentationConfig instrConfig = project.getReference(CloverNames.PROP_CONFIG);
        if (instrConfig == null) {
            instrConfig = new AntInstrumentationConfig(project);
            project.addReference(CloverNames.PROP_CONFIG, instrConfig);
        }
        return instrConfig;
    }



    private Object getConfiguredTask(Task task) {

        final Object proxy = task.getRuntimeConfigurableWrapper().getProxy();
        if (proxy instanceof UnknownElement) {
            UnknownElement ue = (UnknownElement) proxy;
            ue.maybeConfigure();
            return ue.getRealThing();
        } else {
            return task;
        }
    }

    private void injectClover(Javac javac) {
        //add clover to the compile-time classpath
        javac.setClasspath(new Path(javac.getProject(), ClassPathUtil.getCloverJarPath()));
        String origCompiler = javac.getCompiler();
        // set clover compiler adapter as the compiler to use
        javac.setCompiler(CloverCompilerAdapter.class.getName());
        if (origCompiler != null) {
            getConfigForProject(javac.getProject()).setCompilerDelegate(origCompiler);
        }
        instrumentationOccured = true;
    }

    private void injectClover(Java java) {
        executionOccured = true; // we can only assume they are using java to run their instrumented code.
        Path cloverJarPath = java.createClasspath();
        cloverJarPath.add(new Path(java.getProject(), ClassPathUtil.getCloverJarPath()));
    }

    private void injectClover(Javadoc javadoc) {
        executionOccured = true; // at least checkstyle compiles a doclet at build time...

        addCloverToDoclets(javadoc);
        addCloverToTaglets(javadoc);


    }

    private void addCloverToTaglets(Javadoc javadoc) {
        try {
            Field tagsField = javadoc.getClass().getDeclaredField("tagsField");
            tagsField.setAccessible(true);
            final Vector tags = (Vector) tagsField.get(javadoc);
            if (tags != null && tags.size() > 0) {
                for (int i = 0; i < tags.size(); i++) {
                    Javadoc.ExtensionInfo tagInfo  = (Javadoc.ExtensionInfo) tags.elementAt(i);
                    addCloverToPath(javadoc, tagInfo);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Logger.getInstance().debug("Could not inject Clover onto classpath of javadoc task", e);
        }
    }

    private void addCloverToDoclets(Javadoc javadoc) {
        try {
            Field docletField  = javadoc.getClass().getDeclaredField("doclet");
            docletField.setAccessible(true);
            if (docletField.get(javadoc) != null) {
                final Javadoc.ExtensionInfo doclet = javadoc.createDoclet();
                addCloverToPath(javadoc, doclet);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Logger.getInstance().debug("Could not inject Clover onto classpath of javadoc task", e);
        }
    }

    private void addCloverToPath(Javadoc javadoc, Javadoc.ExtensionInfo tagInfo) {
        Path cloverJarPath = tagInfo.createPath();
        cloverJarPath.add(new Path(javadoc.getProject(), ClassPathUtil.getCloverJarPath()));
        javadoc.log("Injected Clover into javadoc doclet classpath: " + cloverJarPath);        
    }

    private void injectClover(JUnitTask junit, boolean optimize) {
        executionOccured = true;
        Path cloverJarPath = junit.createClasspath();
        cloverJarPath.add(new Path(junit.getProject(), ClassPathUtil.getCloverJarPath()));
        if (optimize) {
            injectOptimization(junit);
        }
    }

    private void injectOptimization(JUnitTask junit) {
        Vector<JUnitTest> individualTests;
        Vector<BatchTest> batchTests;
        try {
            Field individualTestsField = junit.getClass().getDeclaredField("tests");
            Field batchTestsField = junit.getClass().getDeclaredField("batchTests");
            individualTestsField.setAccessible(true);
            batchTestsField.setAccessible(true);
            individualTests = (Vector<JUnitTest>)individualTestsField.get(junit);
            batchTests = (Vector<BatchTest>)batchTestsField.get(junit);
            Logger.getInstance().debug("batchTests.size() = " + batchTests.size());
            Logger.getInstance().debug("individualTests.size() = " + individualTests.size());
        }
        catch (IllegalAccessException | NoSuchFieldException e) {
            Logger.getInstance().debug("Unable to access JUnit test field",e);
            Logger.getInstance().warn("Unable to configure JUnit for test optimization");
            return;
        }

        List<OptimizableJUnitTest> wrappedTests = getIndividualWrappedTests(batchTests, individualTests);
        try {
            wrappedTests = LocalSnapshotOptimizer.optimize(wrappedTests, optimizationOptions);
        } catch (CloverException e) {
            Logger.getInstance().debug("Exception when optimizing tests", e);
            Logger.getInstance().error("Optimization failed. Running all specified tests.");
            return;
        }
        individualTests.clear();
        batchTests.clear();
        for (final OptimizableJUnitTest test : wrappedTests) {
            individualTests.add(test.getJUnitTest());
        }
    }

    private static class OptimizableJUnitTest implements Optimizable {

        private JUnitTest test;

        public OptimizableJUnitTest(JUnitTest test) {
            this.test = test;
        }

        @Override
        public String getName() {
            return test.getName();
        }

        public JUnitTest getJUnitTest() {
            return test;
        }
    }

    private List<OptimizableJUnitTest> getIndividualWrappedTests(Vector<BatchTest> batchTests, Vector<JUnitTest> individualTests) {
        final List<OptimizableJUnitTest> wrappedTests = newArrayList();
        for (final BatchTest batchTest : batchTests) {
            for (final Enumeration test = batchTest.elements(); test.hasMoreElements(); ) {
                wrappedTests.add(new OptimizableJUnitTest((JUnitTest) test.nextElement()));
            }
        }
        for (final JUnitTest test : individualTests) {
            wrappedTests.add(new OptimizableJUnitTest(test));
        }
        return wrappedTests;
    }

}

