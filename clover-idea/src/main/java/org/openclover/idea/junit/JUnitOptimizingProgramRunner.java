package org.openclover.idea.junit;

import com.intellij.execution.configurations.ConfigurationInfoProvider;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunnerSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 *
 */
public interface JUnitOptimizingProgramRunner {

    int retrieveJUnitSychSocket(@NotNull JavaParameters javaParameters);

    void replaceJUnitSynchSocket(@NotNull JavaParameters javaParameters, int newSocket);

    @Nullable
    File retrieveTmpFile(@NotNull JavaParameters javaParameters);

    @Nullable
    RunnerSettings createConfigurationData(ConfigurationInfoProvider settingsProvider);
}
