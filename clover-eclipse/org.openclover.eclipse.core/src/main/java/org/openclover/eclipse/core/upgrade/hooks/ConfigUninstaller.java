package org.openclover.eclipse.core.upgrade.hooks;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.openclover.eclipse.core.CloverPlugin;
import org_openclover_runtime.CloverVersionInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openclover.core.util.Lists.newArrayList;

public class ConfigUninstaller {
    private static final String EXTENSION_REGEX = "^(\\s)*osgi\\.framework\\.extensions(\\s)*=(\\s)*(.*)$";
    private static final String OSGI_CONFIG_AREA_PROPERTY = "osgi.configuration.area";
    private static final String CONFIG_INI = "config.ini";
    private static final String EXTENSION_LINE_START = "osgi.framework.extensions=";
    private static final String CSV_TRIMMED_SEP = "(\\s)*,(\\s)*";
    private static final String HOOK_ID = "org.openclover.eclipse.hooks";
    private static final String MESSAGEBOX_UNINSTALL_TITLE = "Clover JDT Hooks Uninstallation";
    private static final String ALREADY_UNINSTALLED_HOOK_THIS_SESSION_PROPERTY = "clover.eclipse.hook.already.uninstalled.this.session";
    private static final String MESSAGEBOX_TITLE = "Clover JDT Hooks Uninstaller";
    private static final String PERMISSION_TEXT =
            "Clover must update the config.ini configuration file of your Eclipse installation.\n\n" +
                    "Click Proceed to allow Clover to perform this action or click Skip if you wish to\n" +
                    "perform this yourself. Instructions on how to do this are provided within the \n" +
                    "online Clover Eclipse documentation.";

    private void patchConfigIni(File configIni, File configIniBackup) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(configIni));
        List<String> lines = new ArrayList<>(50);
        List<String> oldLines = new ArrayList<>(50);

        Pattern extensionsPattern = Pattern.compile(EXTENSION_REGEX);

        //Scan file until a line is found that matches "osgi.framework.extensions=..."
        String line;
        while ((line = reader.readLine()) != null) {
            oldLines.add(line);

            Matcher matcher = extensionsPattern.matcher(line);
            if (matcher.matches()) {
                line = removeExtensionFromLine(matcher);
            }

            if (line != null) {
                lines.add(line);
            }
        }

        swapConfigFile(configIni, configIniBackup, oldLines, lines);

        System.setProperty(ALREADY_UNINSTALLED_HOOK_THIS_SESSION_PROPERTY, Boolean.TRUE.toString());
    }

    private String removeExtensionFromLine(Matcher matcher) {
        String line = null;
        String extensionsString = matcher.group(4);

        List<String> extensions = newArrayList(extensionsString.split(CSV_TRIMMED_SEP));
        extensions.remove(HOOK_ID);

        if (extensions.size() > 0) {
            line = EXTENSION_LINE_START;
            for (int i = 0; i < extensions.size(); i++) {
                String extension = extensions.get(i);
                line += extension;
                if (i < extensions.size() - 1) {
                    line += ",";
                }
            }
        }
        return line;
    }

    private File getBackupConfigIni(File configArea) {
        return new File(configArea, CONFIG_INI + ".uninstall.clover.bak");
    }

    /**
     * @return if succeed in uninstalling the hook
     */
    public boolean run() {
        if (permissionGrantedByUser()) {
            URL configAreaUrl;
            try {
                try {
                    configAreaUrl = new URL(System.getProperty(OSGI_CONFIG_AREA_PROPERTY));
                } catch (MalformedURLException e) {
                    throw new ConfigUpdateAbortedException(
                            "Clover was unable to locate your Eclipse configuration directory.\n" +
                                    "No configuration updates were performed. Please contact support - " + CloverVersionInfo.ATLASSIAN_COM_SUPPORT_RESOURCES,
                            e);
                }

                File configArea = new File(configAreaUrl.getFile());
                if (configArea == null || !configArea.exists() || !configArea.isDirectory() || !configArea.canWrite()) {
                    throw new ConfigUpdateAbortedException(
                            "Clover is unable to write to your Eclipse configuration directory.\n" +
                                    "No configuration updates were performed. Please contact support - " + CloverVersionInfo.ATLASSIAN_COM_SUPPORT_RESOURCES);
                }

                File configIni = new File(configArea, CONFIG_INI);
                File configIniBackup = getBackupConfigIni(configArea);

                if (!configIni.exists()
                        || !configIni.isFile()
                        || !configIni.canWrite()) {
                    throw new ConfigUpdateAbortedException(
                            "Clover is unable to write to your Eclipse configuration file \"config.ini\".\n" +
                                    "No configuration updates were performed. Please contact support - " + CloverVersionInfo.ATLASSIAN_COM_SUPPORT_RESOURCES);
                }

                try {
                    patchConfigIni(configIni, configIniBackup);
                } catch (Exception e) {
                    throw new ConfigUpdateFailedException(
                            "Clover encountered an error while updating Eclipse's conifg.ini.\n" +
                                    "Please contact support - " + CloverVersionInfo.ATLASSIAN_COM_SUPPORT_RESOURCES,
                            e);
                }

                Display.getDefault().syncExec(() ->
                        MessageDialog.openInformation(
                                Display.getDefault().getActiveShell(),
                                MESSAGEBOX_TITLE,
                                "Clover successfully updated your config.ini file.\n" +
                                        "Please restart Eclipse for this change to take effect."));
                return true;
            } catch (final ConfigUpdateException e) {
                Display.getDefault().syncExec(() ->
                        MessageDialog.openError(
                                Display.getDefault().getActiveShell(),
                                MESSAGEBOX_TITLE,
                                e.getMessage()));
            }
        }
        return false;
    }

    public boolean permissionGrantedByUser() {
        final boolean[] permissionGranted = new boolean[]{false};

        Display.getDefault().syncExec(() -> {
            MessageDialog dialog = new MessageDialog(
                    Display.getDefault().getActiveShell(),
                    MESSAGEBOX_TITLE,
                    null, // accept the default window icon
                    PERMISSION_TEXT,
                    MessageDialog.QUESTION,
                    new String[]{IDialogConstants.PROCEED_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
            int dialogResult = dialog.open();

            permissionGranted[0] = (dialogResult == IDialogConstants.OK_ID);
        });

        return permissionGranted[0];
    }

    private void swapConfigFile(File configIni, File configIniBackup, List oldLines, List newLines) throws IOException {
        File tempFile = File.createTempFile("config", "ini");

        writeFile(newLines, tempFile);

        if (configIniBackup.exists()) {
            configIniBackup.delete();
            configIniBackup.createNewFile();
        }

        writeFile(oldLines, configIniBackup);

        writeFile(newLines, configIni);
    }

    private void writeFile(List<String> newLines, File tempFile) throws IOException {
        FileWriter iniFileWriter = new FileWriter(tempFile);
        BufferedWriter iniWriter = new BufferedWriter(iniFileWriter);

        for (String s : newLines) {
            iniWriter.write(s);
            iniWriter.newLine();
        }

        iniWriter.flush();
        iniWriter.close();
    }

    public boolean isHookInstalled() {
        URL configAreaUrl = null;
        try {
            configAreaUrl = new URL(System.getProperty(OSGI_CONFIG_AREA_PROPERTY));

            File configArea = new File(configAreaUrl.getFile());
            if (!configArea.exists() || !configArea.isDirectory() || !configArea.canWrite()) {
                CloverPlugin.logVerbose(
                        "Clover is unable to write to your Eclipse confiuration directory.\n" +
                                "No configuration updates were performed.");
            } else {
                File configIni = new File(configArea, CONFIG_INI);
                if (!configIni.exists()
                        || !configIni.isFile()
                        || !configIni.canWrite()) {
                    CloverPlugin.logVerbose(
                            "Clover is unable to write to your Eclipse configuration file \"config.ini\".\n" +
                                    "No configuration updates were performed.");
                } else {
                    try {
                        return isHookInstalled(configIni);
                    } catch (Exception e) {
                        CloverPlugin.logError(
                                "Clover encountered an error while reading Eclipse's conifg.ini.\n" +
                                        e);
                    }
                }
            }
        } catch (MalformedURLException e) {
            CloverPlugin.logVerbose(
                    "Clover was unable to locate your Eclipse confiuration directory.\n" +
                            "No configuration updates were performed.",
                    e);
        }
        return false;
    }

    private static boolean isHookInstalled(File configIni) throws IOException {
        //If an upgrade is taking place - install(new) | config(new) | uninstall(old)
        //then don't undo the configuration work for the new plugin
        if (!Boolean.parseBoolean(System.getProperty(ALREADY_UNINSTALLED_HOOK_THIS_SESSION_PROPERTY))) {
            BufferedReader reader = new BufferedReader(new FileReader(configIni));
            Pattern extensionsPattern = Pattern.compile(EXTENSION_REGEX);

            //Scan file until a line is found that matches "osgi.framework.extensions=..."
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = extensionsPattern.matcher(line);
                if (matcher.matches()) {
                    return
                            matcher.group(4) != null
                                    && matcher.group(4).contains(HOOK_ID);
                }
            }
        }
        return false;
    }
}
