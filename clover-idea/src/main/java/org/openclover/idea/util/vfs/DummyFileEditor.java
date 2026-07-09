package org.openclover.idea.util.vfs;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.beans.PropertyChangeListener;

public abstract class DummyFileEditor extends UserDataHolderBase implements FileEditor {
    private final VirtualFile file;

    protected DummyFileEditor(@NotNull VirtualFile file) {
        this.file = file;
    }

    @Override
    @NotNull
    public VirtualFile getFile() {
        return file;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return getComponent();
    }

    @Override
    @NotNull
    public FileEditorState getState(@NotNull FileEditorStateLevel fileEditorStateLevel) {
        return (fileEditorState, fileEditorStateLevel1) -> false;
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void selectNotify() {
    }

    @Override
    public void deselectNotify() {
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener propertychangelistener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener propertychangelistener) {
    }

    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Override
    public FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return null;
    }

    @Override
    public void dispose() {
    }
}
