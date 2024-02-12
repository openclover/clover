package org.openclover.idea.autoupdater;

import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.openclover.core.util.ClassPathUtil;
import org.openclover.idea.util.NetUtil;
import org.openclover.idea.util.l10n.CloverIdeaPluginMessages;
import org.openclover.runtime.Logger;
import org.openclover.runtime.util.IOStreamUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class CloverPluginDownloader extends Task.Backgroundable {
    private final String downloadUrl;
    private final Logger LOG = Logger.getInstance("CloverPluginDownloader");

    private File tempFile;

    public CloverPluginDownloader(String downloadUrl) {
        super(null, CloverIdeaPluginMessages.getString("autoupdate.downloading"));
        this.downloadUrl = downloadUrl;
    }


    @Override
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    //@Override
    public void run(@NotNull ProgressIndicator indicator) {
        OutputStream os = null;
        InputStream is = null;
        try {
            tempFile = FileUtil.createTempFile("clover_plugin", ".download");
            tempFile.deleteOnExit();
            try {
                os = new BufferedOutputStream(new FileOutputStream(tempFile));
            } catch (FileNotFoundException e) {
                tempFile.delete();
                throw e;
            }
            try {
                is = new BufferedInputStream(NetUtil.openUrlStream(downloadUrl));
            } catch (IOException e) {
                IOStreamUtils.close(os);
                tempFile.delete();
                throw e;
            }
            for (int b = is.read(); b != -1; b = is.read()) {
                os.write(b);
                if (indicator.isCanceled()) {
                    // isCanceled is cheap, checkCanceled sometimes is not
                    indicator.checkCanceled();
                }
            }

            IOStreamUtils.close(is);
            IOStreamUtils.close(os);

            final File downloadedFile = new File(new URL(downloadUrl).getPath());
            final String currentPath = ClassPathUtil.getCloverJarPath();
            final File currentFile = new File(currentPath);
            if (!currentFile.isFile() || !currentFile.exists()) {
                throw new RuntimeException(CloverIdeaPluginMessages.getFormattedString("autoupdate.wrongcurrentlocation", currentPath));
            }
            final File newFile;
            if (downloadedFile.getName().equals(currentFile.getName())) {
                newFile = new File(currentFile.getParent(), "_" + downloadedFile.getName());
            } else {
                newFile = new File(currentFile.getParent(), downloadedFile.getName());
            }
            LOG.info("Copying " + tempFile + " to " + newFile);
            FileUtil.copy(tempFile, newFile);
            LOG.info("Scheduling deletion of " + currentFile);
            StartupActionScriptManager.addActionCommand(new StartupActionScriptManager.DeleteCommand(currentFile));
            ApplicationManager.getApplication().invokeLater(this::onTrueSuccess);

        } catch (final Exception e) {

            final boolean isCancelled = e instanceof ProcessCanceledException;
            if (!isCancelled) {
                Logger.getInstance().info("Exception during new version download", e);
                ApplicationManager.getApplication().invokeLater(() -> onFailure(e));
            }

            IOStreamUtils.close(os);
            IOStreamUtils.close(is);

            if (tempFile != null) {
                tempFile.delete();
            }

            if (isCancelled) {
                throw (ProcessCanceledException) e;
            }
        }
    }

    protected void onFailure(Exception ex) {
    }

    protected void onTrueSuccess() {
    }
}
