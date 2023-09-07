package com.atlassian.clover.idea.build;

import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.cfg.instr.java.SourceLevel;
import com.atlassian.clover.idea.config.CloverPluginConfig;
import com.intellij.openapi.project.Project;

import java.io.File;

public class BuildUtil {

    public static Class load(String s) {
        try {
            return Class.forName(s);
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }

    public static JavaInstrumentationConfig configureNewInstrumenter(CloverPluginConfig config, Project project, File instDir) {

        JavaInstrumentationConfig instrConfig = new JavaInstrumentationConfig();
        instrConfig.setInitstring(config.getInitString());
        if (instDir != null) {
            instrConfig.setDefaultBaseDir(instDir);
        }
        return internalConfigImp(instrConfig, config, project);
    }

    public static JavaInstrumentationConfig configureNewInstrumenter(CloverPluginConfig config, Project project) {
        return configureNewInstrumenter(config, project, null);
    }

    private static JavaInstrumentationConfig internalConfigImp(JavaInstrumentationConfig instrConfig, CloverPluginConfig config,
                                                           Project project) {
        instrConfig.setFlushPolicy(config.getFlushPolicy());
        instrConfig.setFlushInterval(config.getFlushInterval());
        instrConfig.setSourceLevel(SourceLevel.fromString(config.getLanguageLevel()));
        instrConfig.setTestDetector(new IdeaTestDetector(project));
        instrConfig.setInstrumentLambda(config.getInstrumentLambda());
        return instrConfig;
    }
}
