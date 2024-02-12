package org.openclover.perfmon;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.config.Arguments;

import java.io.File;

public abstract class AbstractJIRASamplerClient extends AbstractCloverSampler {
    protected static final String JIRA_WORKSPACE = "jira.workspace";

    @Override
    public Arguments getDefaultParameters() {
        final Arguments arguments = new Arguments();
        arguments.addArgument(JIRA_WORKSPACE, "${jira.workspace}");
        return arguments;
    }

    protected File resolveJiraHome(JavaSamplerContext context) throws ConfigurationException {
        final File jiraHome = new File(context.getParameter(JIRA_WORKSPACE));
        //One way to sniff out a JIRA project dir
        final File jirasubProjectsDir = new File(jiraHome, "subprojects");
        if (!jirasubProjectsDir.exists() || !jirasubProjectsDir.canRead() || !jirasubProjectsDir.isDirectory()) {
            throw new ConfigurationException("Specified JIRA workspace does not exist, can't be read or is this is not a JIRA workspace: \"" + jiraHome.getAbsolutePath() + "\"");
        } else {
            return jiraHome;
        }
    }
}
