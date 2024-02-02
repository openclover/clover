package org.openclover.idea.junit;

import com.atlassian.clover.Logger;
import org.openclover.idea.junit.config.OptimizedConfigurationSettings;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class FileBasedJUnitClassListProcessor extends JUnitClassListProcessor {

    public FileBasedJUnitClassListProcessor(SavingsReporter savingsReporter, File ideaGeneratedFile, Project currentProject, OptimizedConfigurationSettings optimizationSettings) {
        super(savingsReporter, ideaGeneratedFile, currentProject, optimizationSettings);
    }

    public File processWhenFileNotEmpty() {
        if (ideaGeneratedFile.length() > 0) {
            // idea 8.0- behavior
            processFile();
            return null;
        } else {
            // requires asynchronous processing
            final File processedFile;
            try {
                processedFile = File.createTempFile(TMP_FILE_PREFIX, TMP_FILE_EXT);
            } catch (IOException e) {
                Logger.getInstance().error("Cannot create temporary file", e);
                return null;
            }
            processedFile.deleteOnExit();

            final Task.Backgroundable task = new Task.Backgroundable(currentProject, "Clover Test Optimization Task" , false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    indicator.setText("Waiting for JUnit class list");
                    long lastLength = 0;

                    for (long length = ideaGeneratedFile.length(); length == 0 || length != lastLength; length = ideaGeneratedFile.length()) {
                        lastLength = length;
                        if (!ideaGeneratedFile.exists()) {
                            throw new ProcessCanceledException();
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new ProcessCanceledException(e);
                        }
                    }

                    indicator.setText("Optimizing tests");
                    if (processFile()) {
                        moveFile(ideaGeneratedFile, processedFile);
                    } else {
                        onCancel();
                    }
                }

                @SuppressWarnings({"ResultOfMethodCallIgnored"})
                @Override
                public void onCancel() {
                    ideaGeneratedFile.delete();
                    processedFile.delete();
                }
            };

            task.queue();
            return processedFile;
        }
    }


}
