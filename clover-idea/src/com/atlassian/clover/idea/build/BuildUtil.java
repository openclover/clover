package com.atlassian.clover.idea.build;

import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.idea.config.CloverPluginConfig;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;

public class BuildUtil {

    public static void ensureParentExists(File f) throws IOException {
        if (f == null) {
            throw new IllegalArgumentException();
        }
        File parent = f.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create directory " + parent.getCanonicalPath());
        }
    }

    /**
     *
     * @param s
     * @return
     */
    public static Class load(String s) {
        try {
            return Class.forName(s);
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }

    public static JavaInstrumentationConfig configureNewInstrumenter(CloverPluginConfig config, Project project, File instDir)
            throws CloverException {

        JavaInstrumentationConfig instrConfig = new JavaInstrumentationConfig();
        instrConfig.setInitstring(config.getInitString());
        if (instDir != null) {
            instrConfig.setDefaultBaseDir(instDir);
        }
        return internalConfigImp(instrConfig, config, project);
    }

    public static JavaInstrumentationConfig configureNewInstrumenter(CloverPluginConfig config, Project project)
            throws CloverException {
        return configureNewInstrumenter(config, project, null);
    }

    private static JavaInstrumentationConfig internalConfigImp(JavaInstrumentationConfig instrConfig, CloverPluginConfig config,
                                                           Project project) {
        instrConfig.setFlushPolicy(config.getFlushPolicy());
        instrConfig.setFlushInterval(config.getFlushInterval());
        instrConfig.setSourceLevel(config.getLanguageLevelAsNumber());
        instrConfig.setTestDetector(new IdeaTestDetector(project));
        instrConfig.setInstrumentLambda(config.getInstrumentLambda());
        return instrConfig;
    }
}
