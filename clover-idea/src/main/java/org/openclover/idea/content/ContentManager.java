package org.openclover.idea.content;

import org.openclover.idea.ProjectPlugin;
import org.openclover.idea.config.ConfigChangeEvent;
import org.openclover.idea.config.ConfigChangeListener;
import org.openclover.idea.coverage.EventListenerInstallator;
import org.openclover.idea.feature.CloverFeatures;
import org.openclover.idea.feature.FeatureEvent;
import org.openclover.idea.feature.FeatureListener;
import org.openclover.idea.feature.FeatureManager;
import org.openclover.idea.util.vfs.VfsUtil;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static org.openclover.util.Lists.newLinkedList;
import static org.openclover.util.Maps.newHashMap;

/**
 * The ContentManager is responsible for managing the plugin functionality
 * associated with the FileEditor window of the IDE. This is primarily
 * restricted to the display of coverage information within the editor, in
 * various forms:
 * <ul>
 * <li>Tooltips</li>
 * <li>Highlights</li>
 * <li>Gutter icons</li>
 * <li>Error stripes</li>
 * </ul>
 * <p>The ContentManager provides readOnly functionality that is particular to
 * a project.
 */
public class ContentManager implements FileEditorManagerListener, ConfigChangeListener {

    private final Project project;

    private final Map<VirtualFile, MultiPluginHandler> activePlugins = newHashMap();

    /**
     * @param project current project
     */
    public ContentManager(Project project) {
        this.project = project;
        ProjectPlugin.getPlugin(project).getConfig().addConfigChangeListener(this);
    }

    /**
     * Initialise manager.
     */
    public void init() {
        // init listeners etc as appropriate.
        final FeatureManager fManager = ProjectPlugin.getPlugin(project).getFeatureManager();
        fManager.addFeatureListener(CloverFeatures.CLOVER, evt -> {
            if (evt.isEnabled()) {
                install();
            } else {
                uninstall();
            }
        });

        // if clover enabled, install()
        if (fManager.isFeatureEnabled(CloverFeatures.CLOVER)) {
            install();
        }
    }

    public void cleanup() {
        uninstall();
    }

    /**
     * Enable content related functionality.
     */
    private void install() {
        FileEditorManager fManager = FileEditorManager.getInstance(project);

        // need to install ourselves in all of the currently open file editors.
        for (VirtualFile openFile : fManager.getOpenFiles()) {
            attachPluginTo(fManager, openFile);
        }

        // attach to topic with notifications about file editor changes (open / close file events)
        EventListenerInstallator.install(project, FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
    }

    /**
     * Disable content related funtionality.
     */
    private void uninstall() {
        List<VirtualFile> openFiles = newLinkedList(activePlugins.keySet());
        for (VirtualFile openFile : openFiles) {
            dettachPluginFrom(openFile);
        }
    }

    private void attachPluginTo(FileEditorManager fManager, VirtualFile openFile) {
        //HACK - workaround for IDEA bug IDEA-5875. see http://www.jetbrains.net/jira/browse/IDEA-5875
        if (activePlugins.containsKey(openFile)) {
            dettachPluginFrom(openFile);
        }
        if (!VfsUtil.isFileInJar(openFile)) {

            FileType type = FileTypeManager.getInstance().getFileTypeByFile(openFile);

            if (type instanceof LanguageFileType &&
                    ((LanguageFileType) type).getLanguage().equals(JavaLanguage.INSTANCE)) {

                // find the text editor associated with the open file.
                Editor editor = null;
                FileEditor[] editors = fManager.getEditors(openFile);
                for (FileEditor fileEditor : editors) {
                    if (fileEditor instanceof TextEditor) {
                        editor = ((TextEditor) fileEditor).getEditor();
                        break;
                    }
                }
                if (editor != null) {
                    // create and install the content plugin.
                    MultiPluginHandler plugin = new MultiPluginHandler(project, openFile);
                    plugin.install(editor);
                    activePlugins.put(openFile, plugin);
                }
            }
        }
    }

    private void dettachPluginFrom(VirtualFile openFile) {
        MultiPluginHandler plugin = activePlugins.remove(openFile);
        if (plugin != null) {
            plugin.uninstall();
        }
    }

    //---( Implementation of the FileEditorManagerListener interface )---

    /**
     * @see FileEditorManagerListener#fileOpened(com.intellij.openapi.fileEditor.FileEditorManager, com.intellij.openapi.vfs.VirtualFile)
     */
    @Override
    public void fileOpened(@NotNull FileEditorManager manager, @NotNull VirtualFile openedFile) {
        attachPluginTo(manager, openedFile);
    }

    /**
     * @see FileEditorManagerListener#fileClosed(com.intellij.openapi.fileEditor.FileEditorManager, com.intellij.openapi.vfs.VirtualFile)
     */
    @Override
    public void fileClosed(@NotNull FileEditorManager manager, @NotNull VirtualFile file) {
        dettachPluginFrom(file);
    }

    /**
     * @see FileEditorManagerListener#selectionChanged(com.intellij.openapi.fileEditor.FileEditorManagerEvent)
     */
    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent manager) {

    }

    //---( Implementation of the ConfigChangeListener interface )---

    /**
     * @see ConfigChangeListener#configChange(org.openclover.idea.config.ConfigChangeEvent)
     */
    @Override
    public void configChange(ConfigChangeEvent evt) {

        //TODO: if there are any changes to colours, need to refresh the plugins.
        if (evt.hasPropertyChange("blah")) {
            for (MultiPluginHandler plugin : activePlugins.values()) {
                plugin.refresh();
            }
        }
    }

    static class MultiPluginHandler {

        private final DocMarkupPlugin markupPlugin;
        private final ToolTipPlugin toolTipPlugin;

        public MultiPluginHandler(Project proj, VirtualFile vf) {
            markupPlugin = new DocMarkupPlugin(proj, vf);
            toolTipPlugin = new ToolTipPlugin(proj, vf);
        }

        public void install(Editor editor) {
            markupPlugin.install(editor);
            toolTipPlugin.install(editor);
        }

        public void uninstall() {
            markupPlugin.uninstall();
            toolTipPlugin.uninstall();
        }

        public void refresh() {
            markupPlugin.refresh();
            toolTipPlugin.refresh();
        }
    }
}
