package org.openclover.perfmon;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.config.Arguments;

import java.lang.reflect.Field;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class RunMavenOnJIRA extends AbstractBuildJIRAWithMavenSamplerClient {
    private static final String MAVEN1_ARGUMENTS = "maven1.arguments";
    private static final String MAVEN1_PRE_ARGUMENTS = "maven1.pre.arguments";
    private static final String MAVEN1_POST_ARGUMENTS = "maven1.post.arguments";

    @Override
    public Arguments getDefaultParameters() {
        final Arguments arguments = super.getDefaultParameters();
        arguments.addArgument(MAVEN1_ARGUMENTS, "--info");
        arguments.addArgument(MAVEN1_PRE_ARGUMENTS, "");
        arguments.addArgument(MAVEN1_POST_ARGUMENTS, "");
        return arguments;
    }

    public SampleResult runTest(JavaSamplerContext context) {
        final SampleResult sample = new SampleResult();
        try {
            LaunchResult result;

            final Process process = launchMavenOnJIRA(context, context.getParameter(MAVEN1_PRE_ARGUMENTS).split(" "));

            writePid(context, process);

            pumpToCompletion(process, 4);

            sample.sampleStart();
            try {
                result = pumpToCompletion(launchMavenOnJIRA(context, context.getParameter(MAVEN1_ARGUMENTS).split(" ")), 4);
            } finally {
                sample.sampleEnd();
            }

            pumpToCompletion(launchMavenOnJIRA(context, context.getParameter(MAVEN1_POST_ARGUMENTS).split(" ")), 4);

            sample.setSuccessful(result.getCode() == 0);
            sample.setResponseMessage(result.getLastNLogLines());
        } catch (Exception e) {
            System.out.println("Failed to run test: " + e);
            sample.setSuccessful(false);
            sample.setResponseMessage(e.getMessage());
        }

        return sample;
    }

    private void writePid(JavaSamplerContext context, Process process) throws ConfigurationException, IOException {
        final int pid = getPidFor(process);
        final File pidDir = new File(resolveJiraHome(context), "pids");
        if (!pidDir.exists()) {
            pidDir.mkdir();
        }
        final File pidFile = File.createTempFile("maven1", ".pid", pidDir);
        final FileWriter dos = new FileWriter(pidFile);
        dos.write(Integer.toString(pid));
        dos.close();
    }

    private int getPidFor(Process process) {
        try {
            final Field pidField = process.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            return pidField.getInt(process);
        } catch (Exception e) {
            return -1;
        }
    }
}
