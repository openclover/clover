package org.openclover.ant.types;

import org.openclover.ant.AbstractAntLogger;
import org.openclover.runtime.Logger;
import com.atlassian.clover.api.optimization.OptimizationOptions;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.DataType;

import java.io.File;

public abstract class BaseCloverOptimizedType extends DataType {
    protected File snapshotFile;
    protected OptimizationOptions.Builder optionsBuilder = new OptimizationOptions.Builder().optimizableName("class");

    public void setDebug(boolean debug) {
        optionsBuilder.debug(debug);
    }

    public void setEnabled(boolean enabled) {
        optionsBuilder.enabled(enabled);
    }

    public void setSnapshotFile(File snapshotFile) {
        this.snapshotFile = snapshotFile;
    }

    public void setLogger(Logger logger) {
        optionsBuilder.logger(logger);
    }

    public void setFullRunEvery(int fullRunEvery) {
        optionsBuilder.maxCompilesBeforeStaleSnapshot(fullRunEvery);
    }

    protected Logger takeOverLogging(final Project project) {
        Logger originalLogger = Logger.getInstance();
        Logger logger = optionsBuilder.build().getLogger();
        if (logger == null) {
            final Task currentTask = project.getThreadTask(Thread.currentThread());
            logger = new AbstractAntLogger(project) {
                @Override
                public Task getTask() {
                    return currentTask;
                }
            };
        }
        Logger.setInstance(logger);
        return originalLogger;
    }

    protected Logger revertLogger(Logger logger) {
        Logger orig = Logger.getInstance();
        Logger.setInstance(logger);
        return orig;
    }

    protected static String normalizePath(String resourcePath) {
        resourcePath = resourcePath.replaceAll("\\\\", "/");
        if (resourcePath.endsWith(".class")) {
            int dollarIndex = resourcePath.lastIndexOf("$");
            if (dollarIndex == -1) {
                //Note: Will not work for package-protected test class in a source file not of the same name
                return resourcePath.substring(0, resourcePath.length() - ".class".length()) + ".java";
            } else {
                return resourcePath.substring(0, dollarIndex) + ".java";
            }
        }
        return resourcePath;
    }
}
