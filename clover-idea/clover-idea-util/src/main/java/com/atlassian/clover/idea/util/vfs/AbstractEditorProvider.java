package com.atlassian.clover.idea.util.vfs;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractEditorProvider implements ApplicationComponent, FileEditorProvider {
    private final String editorName;

    protected AbstractEditorProvider(String editorName) {
        this.editorName = editorName;
    }

    @Override
    public void disposeEditor(@NotNull FileEditor fileEditor) {
        Disposer.dispose(fileEditor);
    }

    @Override
    @NotNull
    public FileEditorState readState(@NotNull Element element, @NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new FileEditorState() {
            @Override
            public boolean canBeMergedWith(FileEditorState otherState, FileEditorStateLevel level) {
                return false;
            }
        };
    }

    @Override
    @SuppressWarnings({"NoopMethodInAbstractClass"})
    public void writeState(@NotNull FileEditorState fileEditorState, @NotNull Project project, @NotNull Element element) {
    }

    @Override
    @NotNull
    @NonNls
    public String getEditorTypeId() {
        return editorName + "Report";
    }

    @Override
    @NotNull
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return editorName + "EditorProvider";
    }

    @Override
    @SuppressWarnings({"NoopMethodInAbstractClass"})
    public void initComponent() {
    }

    @Override
    @SuppressWarnings({"NoopMethodInAbstractClass"})
    public void disposeComponent() {
    }
}
