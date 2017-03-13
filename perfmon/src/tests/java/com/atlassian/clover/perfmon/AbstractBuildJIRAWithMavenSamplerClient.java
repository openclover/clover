package com.atlassian.clover.perfmon;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.config.Arguments;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;

public abstract class AbstractBuildJIRAWithMavenSamplerClient extends AbstractJIRASamplerClient {
    private static final String MAVEN1_BASEDIR = "maven1.basedir";
    private static final String MAVEN2_BASEDIR = "maven2.basedir";
    private static final String MAVEN1_HOME_LOCAL = "maven1.home.local";
    private static final String MAVEN1_OPTS = "maven1.opts";
    protected static final String REMOTE_REPO_OFF = "-Dmaven.repo.remote.enabled=false";
    protected static final String MAVEN_EXECUTABLE = "-Dmaven.executable=";

    @Override
    public Arguments getDefaultParameters() {
        final Arguments arguments = super.getDefaultParameters();
        arguments.addArgument(MAVEN1_BASEDIR, "${maven1.basedir}");
        arguments.addArgument(MAVEN2_BASEDIR, "${maven2.basedir}");
        arguments.addArgument(MAVEN1_HOME_LOCAL, "${maven1.home.local}");
        arguments.addArgument(MAVEN1_OPTS, "${maven1.opts}");
        return arguments;
    }

    protected File resolveMaven1Binary(JavaSamplerContext context) throws ConfigurationException {
        final File mavenBasedir= new File(context.getParameter(MAVEN1_BASEDIR));
        final File mavenBin = new File(new File(mavenBasedir, "bin"), "maven");
        if (!mavenBin.exists() || !mavenBin.canRead()) {
            throw new ConfigurationException("Specified Maven 1 basedir does not contain the maven executable or it can't be read: \"" + mavenBasedir.getAbsolutePath() + "\"");
        } else {
            return mavenBin;
        }
    }

    protected File resolveMaven2Binary(JavaSamplerContext context) throws ConfigurationException {
        final File mavenBasedir= new File(context.getParameter(MAVEN2_BASEDIR));
        final File mavenBin = new File(new File(mavenBasedir, "bin"), "mvn");
        if (!mavenBin.exists() || !mavenBin.canRead()) {
            throw new ConfigurationException("Specified Maven 2 basedir does not contain the maven executable or it can't be read: \"" + mavenBasedir.getAbsolutePath() + "\"");
        } else {
            return mavenBin;
        }
    }

    protected File resolveMaven1HomeLocal(JavaSamplerContext context) throws ConfigurationException {
        final File mavenHomeLocal = new File(context.getParameter(MAVEN1_HOME_LOCAL));
        final File mavenHomeLocalRepo = new File(mavenHomeLocal, "repository");
        if (!mavenHomeLocalRepo.exists() || !mavenHomeLocalRepo.canRead()) {
            throw new ConfigurationException("Specified Maven local home can't be read or is not a Maven local home: \"" + mavenHomeLocal.getAbsolutePath() + "\"");
        } else {
            return mavenHomeLocal;
        }
    }

    protected String forLocalHome(File mavenHomeLocal) {
        return "-Dmaven.home.local=" + mavenHomeLocal.getAbsolutePath();
    }

    protected Process launchMavenOnJIRA(JavaSamplerContext context, final String... commands) throws ConfigurationException, IOException {
        if (commands.length > 0 && !(commands.length == 1 && commands[0].trim().length() == 0)) {
            final File maven1Binary = resolveMaven1Binary(context);
            final File maven2Binary = resolveMaven2Binary(context);
            final File maven1HomeLocal = resolveMaven1HomeLocal(context);
            final File jiraHome = resolveJiraHome(context);

            final List<String> commandList = new LinkedList<String>() {
                {
                    add(maven1Binary.getAbsolutePath());
                    add(REMOTE_REPO_OFF);
                    add(forLocalHome(maven1HomeLocal));
                    add(withMaven2(maven2Binary));
                    addAll(Arrays.asList(commands));
                }
            };
            System.out.println("Maven command line: " + commandList);
            System.out.println("Maven opts: " + context.getParameter(MAVEN1_OPTS));
            final ProcessBuilder builder = new ProcessBuilder(commandList.toArray(new String[commandList.size()]))
                .directory(jiraHome.getAbsoluteFile())
                .redirectErrorStream(true);
            builder.environment().put("MAVEN_OPTS", context.getParameter(MAVEN1_OPTS));
            return builder.start();
        } else {
            return null;
        }
    }

    private String withMaven2(File maven2Binary) {
        return "-Dmvn.executable=" + maven2Binary.getAbsolutePath();
    }

    protected LaunchResult pumpToCompletion(Process process, int lastLines) throws IOException, InterruptedException {
        if (process != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            LinkedList<String> recentLines = new LinkedList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                recentLines.addLast(line);
                while (recentLines.size() > lastLines) {
                    recentLines.removeFirst();
                }
                System.out.println(line);
            }

            final int code = process.waitFor();

            StringBuilder message = new StringBuilder("\n");
            for(String recentLine : recentLines) {
                message.append(recentLine);
                message.append("\n");
            }
            return new LaunchResult(code, message.toString());
        } else {
            return new LaunchResult(0, "");
        }
    }

    public static class LaunchResult {
        private int code;
        private String lastNLogLines;

        public LaunchResult(int code, String lastNLogLines) {
            this.code = code;
            this.lastNLogLines = lastNLogLines;
        }

        public int getCode() {
            return code;
        }

        public String getLastNLogLines() {
            return lastNLogLines;
        }
    }
}